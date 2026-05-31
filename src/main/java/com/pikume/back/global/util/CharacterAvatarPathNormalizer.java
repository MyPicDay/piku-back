package com.pikume.back.global.util;

import org.springframework.util.StringUtils;

public final class CharacterAvatarPathNormalizer {

	public static final String FIXED_CHARACTER_PUBLIC_PREFIX = "public/characters/fixed/";
	public static final String LEGACY_FIXED_CHARACTER_PREFIX = "characters/fixed/";

	private CharacterAvatarPathNormalizer() {
	}

	public static String normalizeFixedCharacterObjectKey(String path) {
		if (!StringUtils.hasText(path)) {
			return "";
		}

		String trimmed = path.trim();
		if (isAbsoluteUrl(trimmed)) {
			return trimmed;
		}
		if (trimmed.startsWith(FIXED_CHARACTER_PUBLIC_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedCharacterRelativePath(
					trimmed.substring(FIXED_CHARACTER_PUBLIC_PREFIX.length()));
		}
		if (trimmed.startsWith(LEGACY_FIXED_CHARACTER_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedCharacterRelativePath(
					trimmed.substring(LEGACY_FIXED_CHARACTER_PREFIX.length()));
		}
		return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedCharacterRelativePath(trimmed);
	}

	public static String normalizeAvatarPath(String path) {
		if (!StringUtils.hasText(path)) {
			return "";
		}

		String trimmed = path.trim();
		if (isAbsoluteUrl(trimmed)) {
			return trimmed;
		}
		if (trimmed.startsWith(FIXED_CHARACTER_PUBLIC_PREFIX)
				|| trimmed.startsWith(LEGACY_FIXED_CHARACTER_PREFIX)
				|| !trimmed.contains("/")) {
			return normalizeFixedCharacterObjectKey(trimmed);
		}
		return trimmed;
	}

	public static boolean isAbsoluteUrl(String path) {
		return path != null && (path.startsWith("http://") || path.startsWith("https://"));
	}

	public static boolean isPublicObjectKey(String path) {
		return path != null && path.startsWith("public/");
	}

	public static boolean isFixedCharacterObjectKey(String path) {
		return path != null && path.startsWith(FIXED_CHARACTER_PUBLIC_PREFIX);
	}

	private static String sanitizeFixedCharacterRelativePath(String relativePath) {
		if (!StringUtils.hasText(relativePath)
				|| relativePath.startsWith("/")
				|| relativePath.contains("\\")) {
			throw new IllegalArgumentException("Invalid fixed character image path: " + relativePath);
		}

		for (String segment : relativePath.split("/", -1)) {
			if (!StringUtils.hasText(segment) || segment.equals(".") || segment.equals("..")) {
				throw new IllegalArgumentException("Invalid fixed character image path: " + relativePath);
			}
		}

		String cleanPath = StringUtils.cleanPath(relativePath);
		if (!StringUtils.hasText(cleanPath) || cleanPath.startsWith("/") || cleanPath.contains("\\")) {
			throw new IllegalArgumentException("Invalid fixed character image path: " + relativePath);
		}
		return cleanPath;
	}
}
