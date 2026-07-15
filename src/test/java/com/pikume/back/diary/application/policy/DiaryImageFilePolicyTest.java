package com.pikume.back.diary.application.policy;

import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.application.dto.DiaryPhotoUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DiaryImageFilePolicy")
class DiaryImageFilePolicyTest {

	private final DiaryImageFilePolicy policy = new DiaryImageFilePolicy();

	@Test
	@DisplayName("지원하는 이미지 확장자를 허용한다")
	void acceptsSupportedImageExtensions() {
		assertThatCode(() -> policy.validate(new DiaryPhotoUpload("photo.JPG", "image/jpeg", new byte[] { 1 })))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("파일 이름에 확장자가 없으면 거부한다")
	void rejectsMissingExtension() {
		assertThatThrownBy(() -> policy.validate(new DiaryPhotoUpload("photo", "image/jpeg", new byte[] { 1 })))
				.isInstanceOf(DiaryInvalidRequestException.class)
				.hasMessageContaining("파일 이름");
	}

	@Test
	@DisplayName("지원하지 않는 이미지 확장자를 거부한다")
	void rejectsUnsupportedExtension() {
		assertThatThrownBy(() -> policy.validate(new DiaryPhotoUpload("photo.pdf", "application/pdf", new byte[] { 1 })))
				.isInstanceOf(DiaryInvalidRequestException.class)
				.hasMessageContaining("허용되지 않는");
	}

	@Test
	@DisplayName("내용이 비어 있는 이미지 파일을 거부한다")
	void rejectsEmptyImage() {
		assertThatThrownBy(() -> policy.validate(new DiaryPhotoUpload("photo.jpg", "image/jpeg", new byte[0])))
				.isInstanceOf(DiaryInvalidRequestException.class)
				.hasMessageContaining("비어");
	}
}
