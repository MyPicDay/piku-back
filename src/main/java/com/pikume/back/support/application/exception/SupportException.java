package com.pikume.back.support.application.exception;

import lombok.Getter;

@Getter
public class SupportException extends RuntimeException {

	private final SupportErrorCode errorCode;

	public SupportException(SupportErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public SupportException(SupportErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}
