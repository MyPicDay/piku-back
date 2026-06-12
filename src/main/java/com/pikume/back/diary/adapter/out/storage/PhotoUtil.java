package com.pikume.back.diary.adapter.out.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;

import java.util.Locale;
import java.util.UUID;

import static com.pikume.back.diary.adapter.out.storage.PhotoObjectKeyConstants.PRIVATE_PREFIX;
import static com.pikume.back.diary.adapter.out.storage.PhotoObjectKeyConstants.PUBLIC_PREFIX;

@Slf4j
@Service
public class PhotoUtil {

	private static final String DIARY_IMAGE_ROOT = "diary-images";
	private static final String USER_IMAGE_SEGMENT = "user";
	private static final String AI_IMAGE_SEGMENT = "ai";

	public String generateDiaryUserImageObjectKey(boolean publicAccess, String originalFilename) {
		return generateDiaryImageObjectKey(publicAccess, USER_IMAGE_SEGMENT, extensionFromOriginalFilename(originalFilename));
	}

	public String generateDiaryAiImageObjectKey(String cleanExtension) {
		String extension = normalizeExtension(cleanExtension);
		return generateDiaryImageObjectKey(false, AI_IMAGE_SEGMENT, extension);
	}

	public String publicObjectKeyFor(String objectKey) {
		return objectKeyForPrefix(objectKey, PUBLIC_PREFIX);
	}

	public String visibilityObjectKeyFor(String objectKey, boolean publicAccess, DiaryPhotoType sourceType) {
		if (isDiaryImageObjectKey(objectKey)) {
			return objectKeyForPrefix(objectKey, publicAccess ? PUBLIC_PREFIX : PRIVATE_PREFIX);
		}
		return generateDiaryImageObjectKey(publicAccess, sourceSegment(sourceType), extensionFromObjectKey(objectKey));
	}

	private String generateDiaryImageObjectKey(boolean publicAccess, String sourceSegment, String extension) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String prefix = publicAccess ? PUBLIC_PREFIX : PRIVATE_PREFIX;

		return prefix + DIARY_IMAGE_ROOT + "/"
				+ sourceSegment + "/"
				+ uuid.substring(0, 2) + "/"
				+ uuid.substring(2, 4) + "/"
				+ uuid + extension;
	}

	private String objectKeyForPrefix(String objectKey, String targetPrefix) {
		if (objectKey == null || objectKey.isBlank()) {
			throw new IllegalArgumentException("objectKey는 필수입니다.");
		}
		if (objectKey.startsWith(targetPrefix)) {
			return objectKey;
		}
		if (objectKey.startsWith(PUBLIC_PREFIX)) {
			return targetPrefix + objectKey.substring(PUBLIC_PREFIX.length());
		}
		if (objectKey.startsWith(PRIVATE_PREFIX)) {
			return targetPrefix + objectKey.substring(PRIVATE_PREFIX.length());
		}
		return targetPrefix + objectKey;
	}

	private boolean isDiaryImageObjectKey(String objectKey) {
		return objectKey != null
				&& (objectKey.startsWith(PUBLIC_PREFIX + DIARY_IMAGE_ROOT + "/")
				|| objectKey.startsWith(PRIVATE_PREFIX + DIARY_IMAGE_ROOT + "/"));
	}

	private String sourceSegment(DiaryPhotoType sourceType) {
		if (sourceType == DiaryPhotoType.AI_IMAGE) {
			return AI_IMAGE_SEGMENT;
		}
		return USER_IMAGE_SEGMENT;
	}

	private String extensionFromOriginalFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return "";
		}
		int dotIndex = originalFilename.lastIndexOf(".");
		if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
			return "";
		}
		return normalizeExtension(originalFilename.substring(dotIndex + 1));
	}

	private String extensionFromObjectKey(String objectKey) {
		if (objectKey == null || objectKey.isBlank()) {
			return "";
		}
		int queryIndex = objectKey.indexOf('?');
		String path = queryIndex >= 0 ? objectKey.substring(0, queryIndex) : objectKey;
		int slashIndex = path.lastIndexOf('/');
		String filename = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
		return extensionFromOriginalFilename(filename);
	}

	private String normalizeExtension(String extension) {
		if (extension == null || extension.isBlank()) {
			return "";
		}
		String normalized = extension.startsWith(".") ? extension.substring(1) : extension;
		return "." + normalized.toLowerCase(Locale.ROOT);
	}

}
