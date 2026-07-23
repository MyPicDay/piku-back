package com.pikume.back.character.domain.vo;

public record CharacterImageReference(String value) {

	public CharacterImageReference {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("캐릭터 이미지 참조는 비어 있을 수 없습니다.");
		}
		value = value.trim();
	}

	public static CharacterImageReference of(String value) {
		return new CharacterImageReference(value);
	}

	public boolean isAbsoluteUrl() {
		return value.startsWith("http://") || value.startsWith("https://");
	}
}
