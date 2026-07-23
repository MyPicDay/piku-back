package com.pikume.back.user.application.dto;

public record UserAvatarReference(
		String value,
		boolean absoluteUrl,
		boolean publiclyAccessible
) {

	private static final String FIXED_CHARACTER_PUBLIC_PREFIX = "public/characters/fixed/";
	private static final String LEGACY_FIXED_CHARACTER_PREFIX = "characters/fixed/";

	public static UserAvatarReference fromStoredPath(String storedPath) {
		if (storedPath == null || storedPath.isBlank()) {
			return empty();
		}

		String trimmed = storedPath.trim();
		if (isAbsoluteUrl(trimmed)) {
			return new UserAvatarReference(trimmed, true, false);
		}

		try {
			String objectKey = normalizeObjectKey(trimmed);
			return new UserAvatarReference(objectKey, false, objectKey.startsWith("public/"));
		} catch (IllegalArgumentException exception) {
			return empty();
		}
	}

	public boolean isEmpty() {
		return value.isEmpty();
	}

	private static UserAvatarReference empty() {
		return new UserAvatarReference("", false, false);
	}

	private static String normalizeObjectKey(String path) {
		if (path.startsWith(FIXED_CHARACTER_PUBLIC_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedCharacterPath(
					path.substring(FIXED_CHARACTER_PUBLIC_PREFIX.length()));
		}
		if (path.startsWith(LEGACY_FIXED_CHARACTER_PREFIX)) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedCharacterPath(
					path.substring(LEGACY_FIXED_CHARACTER_PREFIX.length()));
		}
		if (!path.contains("/")) {
			return FIXED_CHARACTER_PUBLIC_PREFIX + sanitizeFixedCharacterPath(path);
		}
		return path;
	}

	private static String sanitizeFixedCharacterPath(String relativePath) {
		if (relativePath == null
				|| relativePath.isBlank()
				|| relativePath.startsWith("/")
				|| relativePath.contains("\\")) {
			throw new IllegalArgumentException("Invalid fixed character image path");
		}
		for (String segment : relativePath.split("/", -1)) {
			if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
				throw new IllegalArgumentException("Invalid fixed character image path");
			}
		}
		return relativePath;
	}

	private static boolean isAbsoluteUrl(String path) {
		return path.startsWith("http://") || path.startsWith("https://");
	}
}
