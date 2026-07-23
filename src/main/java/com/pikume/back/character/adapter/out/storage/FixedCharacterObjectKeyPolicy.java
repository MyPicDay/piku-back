package com.pikume.back.character.adapter.out.storage;

import com.pikume.back.character.application.port.out.CanonicalizeFixedCharacterObjectKeyPort;
import org.springframework.stereotype.Component;

@Component
public class FixedCharacterObjectKeyPolicy implements CanonicalizeFixedCharacterObjectKeyPort {

	public static final String FIXED_CHARACTER_PUBLIC_PREFIX = "public/characters/fixed/";
	private static final String LEGACY_FIXED_CHARACTER_PREFIX = "characters/fixed/";

	@Override
	public String canonicalizeFixedCharacterObjectKey(String storedReference) {
		if (storedReference == null || storedReference.isBlank()) {
			return "";
		}

		String trimmed = storedReference.trim();
		if (isAbsoluteUrl(trimmed)) {
			return trimmed;
		}
		if (trimmed.startsWith(FIXED_CHARACTER_PUBLIC_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeRelativePath(
					trimmed.substring(FIXED_CHARACTER_PUBLIC_PREFIX.length()));
		}
		if (trimmed.startsWith(LEGACY_FIXED_CHARACTER_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeRelativePath(
					trimmed.substring(LEGACY_FIXED_CHARACTER_PREFIX.length()));
		}
		return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeRelativePath(trimmed);
	}

	private boolean isAbsoluteUrl(String value) {
		return value.startsWith("http://") || value.startsWith("https://");
	}

	private String sanitizeRelativePath(String relativePath) {
		if (relativePath == null
				|| relativePath.isBlank()
				|| relativePath.startsWith("/")
				|| relativePath.contains("\\")) {
			throw new IllegalArgumentException("유효하지 않은 고정 캐릭터 이미지 경로입니다.");
		}
		for (String segment : relativePath.split("/", -1)) {
			if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
				throw new IllegalArgumentException("유효하지 않은 고정 캐릭터 이미지 경로입니다.");
			}
		}
		return relativePath;
	}
}
