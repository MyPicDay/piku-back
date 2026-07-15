package com.pikume.back.diary.domain;

import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Photo")
class PhotoTest {

	@Test
	@DisplayName("표시 경로는 optimizedUrl이 있으면 optimizedUrl을 우선 사용한다")
	void displayUrlPrefersOptimizedUrl() {
		Photo photo = new Photo(diary(), "user-1/photo.png", 0);

		photo.markOptimizationSucceeded("user-1/photo.webp");

		assertThat(photo.getDisplayUrl()).isEqualTo("user-1/photo.webp");
		assertThat(photo.getUrl()).isEqualTo("user-1/photo.png");
	}

	@Test
	@DisplayName("표시 경로는 optimizedUrl이 없으면 원본 url을 사용한다")
	void displayUrlFallsBackToOriginalUrl() {
		Photo photo = new Photo(diary(), "user-1/photo.png", 0);

		assertThat(photo.getDisplayUrl()).isEqualTo("user-1/photo.png");
	}

	@Test
	@DisplayName("변환 대상 확장자는 PENDING 상태로 초기화한다")
	void initializesConvertibleImagesAsPending() {
		Photo photo = new Photo(diary(), "user-1/photo.PNG", 0);

		assertThat(photo.getOptimizationStatus()).isEqualTo(PhotoOptimizationStatus.PENDING);
		assertThat(photo.getOptimizedUrl()).isNull();
	}

	@Test
	@DisplayName("이미 WebP인 이미지는 원본 경로를 optimizedUrl로 초기화한다")
	void initializesWebpImagesAsSucceeded() {
		Photo photo = new Photo(diary(), "user-1/photo.webp", 0);

		assertThat(photo.getOptimizationStatus()).isEqualTo(PhotoOptimizationStatus.SUCCEEDED);
		assertThat(photo.getOptimizedUrl()).isEqualTo("user-1/photo.webp");
	}

	@Test
	@DisplayName("변환하지 않을 확장자는 SKIPPED 상태로 초기화한다")
	void initializesUnsupportedImagesAsSkipped() {
		Photo photo = new Photo(diary(), "user-1/photo.gif", 0);

		assertThat(photo.getOptimizationStatus()).isEqualTo(PhotoOptimizationStatus.SKIPPED);
		assertThat(photo.getOptimizedUrl()).isNull();
	}

	@Test
	@DisplayName("삭제된 일기의 사진은 최적화 대상이 아니다")
	void deletedDiaryPhotoIsNotOptimizationCandidate() {
		Diary diary = diary();
		Photo photo = new Photo(diary, "user-1/photo.png", 0);
		diary.delete();

		assertThat(photo.isOptimizationCandidate()).isFalse();
	}

	@Test
	@DisplayName("사진은 부모 일기와 object key 없이 생성할 수 없다")
	void rejectsMissingDiaryOrObjectKey() {
		assertThatThrownBy(() -> new Photo(null, "user-1/photo.png", 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Photo(diary(), " ", 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private Diary diary() {
		return new Diary("content", DiaryVisibility.PUBLIC, LocalDate.of(2026, 6, 3), "user-1");
	}
}
