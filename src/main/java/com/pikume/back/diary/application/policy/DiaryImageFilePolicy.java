package com.pikume.back.diary.application.policy;

import com.pikume.back.diary.application.dto.DiaryPhotoUpload;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class DiaryImageFilePolicy {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");

	public void validate(DiaryPhotoUpload upload) {
		if (upload == null || upload.originalFilename() == null) {
			throw new DiaryInvalidRequestException("유효하지 않은 파일 이름입니다.");
		}
		if (upload.bytes().length == 0) {
			throw new DiaryInvalidRequestException("이미지 파일은 비어 있을 수 없습니다.");
		}
		String filename = upload.originalFilename();
		int extensionSeparator = filename.lastIndexOf('.');
		if (extensionSeparator < 0 || extensionSeparator == filename.length() - 1) {
			throw new DiaryInvalidRequestException("유효하지 않은 파일 이름입니다: " + filename);
		}
		String extension = filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new DiaryInvalidRequestException("허용되지 않는 이미지 확장자입니다: " + filename);
		}
	}
}
