package com.pikume.back.creative.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GenerateDiaryImageRequest(
		@NotBlank
		@Schema(description = "AI 이미지로 표현할 일기 내용")
		String content
) {
}
