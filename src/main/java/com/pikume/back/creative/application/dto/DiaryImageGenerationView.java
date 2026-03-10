package com.pikume.back.creative.application.dto;

public record DiaryImageGenerationView(
		Long id,
		String userId,
		String prompt,
		String filePath,
		Long diaryId) {
}
