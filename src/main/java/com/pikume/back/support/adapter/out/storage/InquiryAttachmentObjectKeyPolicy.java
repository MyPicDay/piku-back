package com.pikume.back.support.adapter.out.storage;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class InquiryAttachmentObjectKeyPolicy {

	public String createObjectKey(String userId, String originalFilename, LocalDate date) {
		String userSegment = safeUserSegment(userId);
		String extension = safeExtension(originalFilename);
		return "inquiry/" + date + "/" + userSegment + "_" + UUID.randomUUID() + extension;
	}

	private String safeUserSegment(String userId) {
		String normalized = userId == null ? "" : userId.replaceAll("[^A-Za-z0-9_-]", "_");
		if (normalized.isBlank()) {
			return "unknown";
		}
		return normalized.substring(0, Math.min(8, normalized.length()));
	}

	private String safeExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		int extensionIndex = originalFilename.lastIndexOf('.');
		if (extensionIndex < 0 || extensionIndex == originalFilename.length() - 1) {
			return "";
		}
		String extension = originalFilename.substring(extensionIndex + 1);
		return extension.matches("[A-Za-z0-9]{1,10}") ? "." + extension : "";
	}
}
