package com.pikume.back.creative.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GenerateDiaryImageRequest(
		@NotBlank
		@Schema(description = "AI 이미지로 표현할 일기 내용")
		String content,
		@Positive(message = "캐릭터 식별자는 양수여야 합니다.")
		@Schema(description = "이미지 생성에 사용할 선택 캐릭터 식별자", nullable = true)
		Long characterId
) {
}
