package com.pikume.back.diary.application.dto;

public record PhotoOptimizationTarget(
		Integer photoId,
		Long diaryId,
		String originalUrl,
		int attemptCount
) {
}
