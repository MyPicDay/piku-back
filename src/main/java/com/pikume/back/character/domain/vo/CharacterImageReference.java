package com.pikume.back.character.domain.vo;

import java.util.Optional;

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

	public Optional<String> toUsableObjectKey() {
		if (isAbsoluteUrl() || value.startsWith("/") || value.contains("\\")) {
			return Optional.empty();
		}
		for (String segment : value.split("/", -1)) {
			if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
				return Optional.empty();
			}
		}
		return Optional.of(value);
	}
}
