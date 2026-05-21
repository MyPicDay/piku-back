package com.pikume.back.user.application.exception;

import lombok.Getter;

@Getter
public class UserException extends RuntimeException {

	private final UserErrorCode errorCode;

	public UserException(UserErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public UserException(UserErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public UserException(UserErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
}
