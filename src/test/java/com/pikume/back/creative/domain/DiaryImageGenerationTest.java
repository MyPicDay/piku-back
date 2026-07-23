package com.pikume.back.creative.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DiaryImageGeneration")
class DiaryImageGenerationTest {

	@Test
	@DisplayName("생성 이력은 사용자, 프롬프트와 이미지 참조가 필요하다")
	void requiresGenerationFacts() {
		assertThatThrownBy(() -> DiaryImageGeneration.create(" ", "prompt", "path"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DiaryImageGeneration.create("user-1", " ", "path"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DiaryImageGeneration.create("user-1", "prompt", " "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("생성 이력은 하나의 일기에만 연결할 수 있다")
	void attachesToOnlyOneDiary() {
		DiaryImageGeneration generation =
				DiaryImageGeneration.create("user-1", "prompt", "private/image.png");

		generation.attachToDiary(1L);

		assertThat(generation.getDiaryId()).isEqualTo(1L);
		assertThatThrownBy(() -> generation.attachToDiary(2L))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("폐기된 생성 이력은 일기에 연결할 수 없다")
	void discardedGenerationCannotBeAttached() {
		DiaryImageGeneration generation =
				DiaryImageGeneration.create("user-1", "prompt", "private/image.png");
		generation.discard();

		assertThatThrownBy(() -> generation.attachToDiary(1L))
				.isInstanceOf(IllegalStateException.class);
	}
}
