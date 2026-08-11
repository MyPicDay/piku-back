package com.pikume.back.user.application.dto;

public record AvatarCharacterSelection(String userId, Long characterId) {

	public AvatarCharacterSelection {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("사용자 식별자는 비어 있을 수 없습니다.");
		}
		if (characterId == null || characterId <= 0) {
			throw new IllegalArgumentException("아바타 캐릭터 식별자는 양수여야 합니다.");
		}
		userId = userId.trim();
	}
}
