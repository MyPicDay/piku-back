package com.pikume.back.creative.adapter.out.storage;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class CreativeImageObjectKeyPolicy {

	private static final String PRIVATE_AI_IMAGE_PREFIX = "private/diary-images/ai/";

	public String createGeneratedImageObjectKey(String fileExtension) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		return PRIVATE_AI_IMAGE_PREFIX
				+ uuid.substring(0, 2) + "/"
				+ uuid.substring(2, 4) + "/"
				+ uuid
				+ normalizeExtension(fileExtension);
	}

	public String contentType(String fileExtension) {
		return switch (normalizeExtension(fileExtension)) {
			case ".jpg", ".jpeg" -> "image/jpeg";
			case ".webp" -> "image/webp";
			case ".gif" -> "image/gif";
			default -> "image/png";
		};
	}

	private String normalizeExtension(String fileExtension) {
		if (fileExtension == null || fileExtension.isBlank()) {
			return "";
		}
		String normalized = fileExtension.startsWith(".")
				? fileExtension.substring(1)
				: fileExtension;
		return "." + normalized.toLowerCase(Locale.ROOT);
	}
}
