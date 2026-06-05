package com.pikume.back.diary.domain;

import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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

	private Diary diary() {
		return new Diary("content", DiaryVisibility.PUBLIC, LocalDate.of(2026, 6, 3), "user-1");
	}
}
