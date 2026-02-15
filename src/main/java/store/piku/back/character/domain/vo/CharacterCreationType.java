package store.piku.back.character.domain.vo;

import lombok.Getter;

/**
 * 캐릭터 생성 유형 Value Object
 */
@Getter
public enum CharacterCreationType {
	FIXED("고정"),
	AI_GENERATED("AI 생성");

	private final String description;

	CharacterCreationType(String description) {
		this.description = description;
	}
}
