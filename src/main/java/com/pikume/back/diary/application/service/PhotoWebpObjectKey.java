package com.pikume.back.diary.application.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class PhotoWebpObjectKey {

	private static final Set<String> CONVERTIBLE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "bmp");

	private PhotoWebpObjectKey() {
	}

	static Optional<String> fromOriginal(String objectKey) {
		if (objectKey == null || objectKey.isBlank()) {
			return Optional.empty();
		}

		int dotIndex = objectKey.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == objectKey.length() - 1) {
			return Optional.empty();
		}

		String extension = objectKey.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
		if (!CONVERTIBLE_EXTENSIONS.contains(extension)) {
			return Optional.empty();
		}

		return Optional.of(objectKey.substring(0, dotIndex) + ".webp");
	}
}
