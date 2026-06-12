package com.pikume.back.diary.application.exception;

public class DiaryImageRelocationException extends DiaryException {

	public DiaryImageRelocationException(String message) {
		super(DiaryErrorCode.DIARY_IMAGE_RELOCATION_FAILED, message);
	}

	public DiaryImageRelocationException(String message, Throwable cause) {
		super(DiaryErrorCode.DIARY_IMAGE_RELOCATION_FAILED, message, cause);
	}
}
