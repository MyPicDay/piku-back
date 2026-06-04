package com.pikume.back.diary.application.exception;

public class DiaryInvalidRequestException extends DiaryException {
	public DiaryInvalidRequestException(String message) {
		super(DiaryErrorCode.DIARY_INVALID_REQUEST, message);
	}
}
