package com.pikume.back.character.application.dto;

public record CharacterImageReferenceResult(
		String userId,
		Long characterId,
		String imageReference,
		boolean absoluteUrl,
		boolean publiclyAccessible
) {

	public CharacterImageReferenceResult {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("사용자 식별자는 비어 있을 수 없습니다.");
		}
		if (characterId == null || characterId <= 0) {
			throw new IllegalArgumentException("캐릭터 식별자는 양수여야 합니다.");
		}
		if (imageReference == null || imageReference.isBlank()) {
			throw new IllegalArgumentException("캐릭터 이미지 참조는 비어 있을 수 없습니다.");
		}
		userId = userId.trim();
		imageReference = imageReference.trim();
	}
}
