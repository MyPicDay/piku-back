package store.piku.back.character.enums;

import lombok.Getter;

@Getter
public enum CharacterCreationType {
    FIXED("고정"), // 미리 정의된 캐릭터 (현재 4개)
    AI_GENERATED("AI 생성"); // AI를 통해 생성된 캐릭터 (사용자 프롬프트 기반)

    private final String description;

    CharacterCreationType(String description) {
        this.description = description;
    }

}