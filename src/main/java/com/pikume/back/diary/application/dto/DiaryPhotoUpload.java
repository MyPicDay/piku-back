package com.pikume.back.diary.application.dto;

public record DiaryPhotoUpload(String originalFilename, String contentType, byte[] bytes) {

	public DiaryPhotoUpload {
		bytes = bytes == null ? new byte[0] : bytes.clone();
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}
}
