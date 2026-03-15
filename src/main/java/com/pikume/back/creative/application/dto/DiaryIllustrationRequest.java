package com.pikume.back.creative.application.dto;

public record DiaryIllustrationRequest(
		String prompt,
		String referenceImageBase64) {
}
