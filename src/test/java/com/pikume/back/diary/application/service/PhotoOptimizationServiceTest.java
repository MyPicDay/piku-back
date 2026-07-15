package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;
import com.pikume.back.diary.application.port.out.LoadPhotoOptimizationPort;
import com.pikume.back.diary.application.port.out.LoadDiaryPhotoObjectPort;
import com.pikume.back.diary.application.port.out.SavePhotoOptimizationPort;
import com.pikume.back.diary.application.port.out.StoreOptimizedDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.WebpImageConversionPort;
import com.pikume.back.diary.domain.PhotoOptimizationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhotoOptimizationService")
class PhotoOptimizationServiceTest {

	@Mock
	private LoadPhotoOptimizationPort loadPhotoOptimizationPort;
	@Mock
	private SavePhotoOptimizationPort savePhotoOptimizationPort;
	@Mock
	private LoadDiaryPhotoObjectPort loadObjectPort;
	@Mock
	private StoreOptimizedDiaryPhotoPort storeObjectPort;
	@Mock
	private WebpImageConversionPort webpImageConversionPort;

	@Test
	@DisplayName("pending 사진을 claim한 뒤 같은 prefix/baseName의 public WebP로 저장하고 성공 처리한다")
	void optimizesPendingPublicPhoto() {
		PhotoOptimizationProperties properties = properties();
		PhotoOptimizationService service = service(properties);
		byte[] originalBytes = "png".getBytes();
		byte[] webpBytes = "webp".getBytes();
		given(loadPhotoOptimizationPort.findPendingPhotoOptimizationTargets(10))
				.willReturn(List.of(new PhotoOptimizationTarget(1, 11L, "public/user-1/photo.png", 0)));
		given(savePhotoOptimizationPort.claimPhotoOptimization(eq(1), any(LocalDateTime.class)))
				.willReturn(true);
		given(loadObjectPort.load("public/user-1/photo.png")).willReturn(originalBytes);
		given(webpImageConversionPort.convertToWebp(originalBytes, 0.82f)).willReturn(webpBytes);

		int optimizedCount = service.optimizePendingPhotos();

		assertThat(optimizedCount).isEqualTo(1);
		then(storeObjectPort).should().store("public/user-1/photo.webp", "image/webp", webpBytes);
		then(savePhotoOptimizationPort).should()
				.markPhotoOptimizationSucceeded(eq(1), eq("public/user-1/photo.webp"), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("claim에 실패한 사진은 storage를 읽지 않는다")
	void skipsTargetWhenClaimFails() {
		PhotoOptimizationService service = service(properties());
		given(loadPhotoOptimizationPort.findPendingPhotoOptimizationTargets(10))
				.willReturn(List.of(new PhotoOptimizationTarget(1, 11L, "user-1/photo.png", 0)));
		given(savePhotoOptimizationPort.claimPhotoOptimization(eq(1), any(LocalDateTime.class)))
				.willReturn(false);

		int optimizedCount = service.optimizePendingPhotos();

		assertThat(optimizedCount).isZero();
		then(loadObjectPort).shouldHaveNoInteractions();
		then(storeObjectPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("변환 실패 시 최대 시도 전이면 다시 PENDING 상태로 둔다")
	void marksFailureAsPendingWhenAttemptsRemain() {
		PhotoOptimizationService service = service(properties());
		given(loadPhotoOptimizationPort.findPendingPhotoOptimizationTargets(10))
				.willReturn(List.of(new PhotoOptimizationTarget(1, 11L, "user-1/photo.png", 1)));
		given(savePhotoOptimizationPort.claimPhotoOptimization(eq(1), any(LocalDateTime.class)))
				.willReturn(true);
		given(loadObjectPort.load("user-1/photo.png")).willReturn("png".getBytes());
		given(webpImageConversionPort.convertToWebp(any(), eq(0.82f))).willThrow(new RuntimeException("convert failed"));

		int optimizedCount = service.optimizePendingPhotos();

		assertThat(optimizedCount).isZero();
		then(savePhotoOptimizationPort).should()
				.markPhotoOptimizationFailed(eq(1), eq(PhotoOptimizationStatus.PENDING), any(LocalDateTime.class));
		then(savePhotoOptimizationPort).should(never())
				.markPhotoOptimizationSucceeded(any(), any(), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("변환 실패 시 최대 시도에 도달하면 FAILED 상태로 둔다")
	void marksFailureAsFailedWhenMaxAttemptsReached() {
		PhotoOptimizationService service = service(properties());
		given(loadPhotoOptimizationPort.findPendingPhotoOptimizationTargets(10))
				.willReturn(List.of(new PhotoOptimizationTarget(1, 11L, "user-1/photo.png", 2)));
		given(savePhotoOptimizationPort.claimPhotoOptimization(eq(1), any(LocalDateTime.class)))
				.willReturn(true);
		given(loadObjectPort.load("user-1/photo.png")).willReturn("png".getBytes());
		given(webpImageConversionPort.convertToWebp(any(), eq(0.82f))).willThrow(new RuntimeException("convert failed"));

		service.optimizePendingPhotos();

		then(savePhotoOptimizationPort).should()
				.markPhotoOptimizationFailed(eq(1), eq(PhotoOptimizationStatus.FAILED), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("WebP key를 만들 수 없는 pending row는 SKIPPED 처리한다")
	void marksTargetAsSkippedWhenWebpKeyCannotBeBuilt() {
		PhotoOptimizationService service = service(properties());
		given(loadPhotoOptimizationPort.findPendingPhotoOptimizationTargets(10))
				.willReturn(List.of(new PhotoOptimizationTarget(1, 11L, "user-1/photo", 0)));
		given(savePhotoOptimizationPort.claimPhotoOptimization(eq(1), any(LocalDateTime.class)))
				.willReturn(true);

		int optimizedCount = service.optimizePendingPhotos();

		assertThat(optimizedCount).isZero();
		then(savePhotoOptimizationPort).should()
				.markPhotoOptimizationSkipped(eq(1), any(LocalDateTime.class));
		then(loadObjectPort).shouldHaveNoInteractions();
	}

	private PhotoOptimizationService service(PhotoOptimizationProperties properties) {
		return new PhotoOptimizationService(
				loadPhotoOptimizationPort,
				savePhotoOptimizationPort,
				loadObjectPort,
				storeObjectPort,
				webpImageConversionPort,
				properties);
	}

	private PhotoOptimizationProperties properties() {
		PhotoOptimizationProperties properties = new PhotoOptimizationProperties();
		properties.setBatchSize(10);
		properties.setMaxAttempts(3);
		properties.setQuality(0.82f);
		return properties;
	}
}
