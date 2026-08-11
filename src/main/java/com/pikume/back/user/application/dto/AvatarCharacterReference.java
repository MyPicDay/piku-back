package com.pikume.back.user.application.dto;

public record AvatarCharacterReference(
		String userId,
		Long characterId,
		UserAvatarReference imageReference
) {

	public AvatarCharacterReference {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("사용자 식별자는 비어 있을 수 없습니다.");
		}
		if (characterId == null || characterId <= 0) {
			throw new IllegalArgumentException("아바타 캐릭터 식별자는 양수여야 합니다.");
		}
		if (imageReference == null) {
			throw new IllegalArgumentException("아바타 캐릭터 이미지 참조는 비어 있을 수 없습니다.");
		}
		userId = userId.trim();
	}

	public AvatarCharacterSelection selection() {
		return new AvatarCharacterSelection(userId, characterId);
	}
}
