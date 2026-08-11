package com.pikume.back.creative.application.dto;

public record GenerateDiaryImageCommand(
		String content,
		String userId,
		Long characterId
) {
}
