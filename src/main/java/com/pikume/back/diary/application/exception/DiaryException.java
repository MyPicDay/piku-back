package com.pikume.back.diary.application.exception;

import lombok.Getter;

@Getter
public class DiaryException extends RuntimeException {

	private final DiaryErrorCode errorCode;

	public DiaryException(DiaryErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public DiaryException(DiaryErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
