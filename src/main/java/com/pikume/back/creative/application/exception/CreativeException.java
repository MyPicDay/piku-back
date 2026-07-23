package com.pikume.back.creative.application.exception;

import lombok.Getter;

@Getter
public class CreativeException extends RuntimeException {

	private final CreativeErrorCode errorCode;

	public CreativeException(CreativeErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public CreativeException(CreativeErrorCode errorCode, String detail) {
		super(detail);
		this.errorCode = errorCode;
	}

	public CreativeException(CreativeErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}
