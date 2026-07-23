package com.pikume.back.creative.application.policy;

import org.springframework.stereotype.Component;

@Component
public class CharacterReferencePolicy {

	private static final String FIXED_CHARACTER_PUBLIC_PREFIX = "public/characters/fixed/";
	private static final String LEGACY_FIXED_CHARACTER_PREFIX = "characters/fixed/";

	public String canonicalize(String storedReference) {
		if (storedReference == null || storedReference.isBlank()) {
			return "";
		}
		String trimmed = storedReference.trim();
		if (isAbsoluteUrl(trimmed)) {
			return trimmed;
		}
		if (trimmed.startsWith(FIXED_CHARACTER_PUBLIC_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedReference(
					trimmed.substring(FIXED_CHARACTER_PUBLIC_PREFIX.length()));
		}
		if (trimmed.startsWith(LEGACY_FIXED_CHARACTER_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedReference(
					trimmed.substring(LEGACY_FIXED_CHARACTER_PREFIX.length()));
		}
		if (!trimmed.contains("/")) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedReference(trimmed);
		}
		return trimmed;
	}

	public boolean isAbsoluteUrl(String reference) {
		return reference != null
				&& (reference.startsWith("http://") || reference.startsWith("https://"));
	}

	private String sanitizeFixedReference(String relativePath) {
		if (relativePath == null
				|| relativePath.isBlank()
				|| relativePath.startsWith("/")
				|| relativePath.contains("\\")) {
			throw new IllegalArgumentException("유효하지 않은 캐릭터 참조입니다.");
		}
		for (String segment : relativePath.split("/", -1)) {
			if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
				throw new IllegalArgumentException("유효하지 않은 캐릭터 참조입니다.");
			}
		}
		return relativePath;
	}
}
