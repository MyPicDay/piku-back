package com.pikume.back.user.application.exception;

public class UserNotFoundException extends UserException {
	public UserNotFoundException() {
		super(UserErrorCode.USER_NOT_FOUND);
	}

	public UserNotFoundException(String message) {
		super(UserErrorCode.USER_NOT_FOUND, message);
	}

	public UserNotFoundException(String message, Throwable cause) {
		super(UserErrorCode.USER_NOT_FOUND, message, cause);
	}
}
