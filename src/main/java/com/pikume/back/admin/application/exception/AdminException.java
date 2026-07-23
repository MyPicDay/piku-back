package com.pikume.back.admin.application.exception;

public class AdminException extends RuntimeException {

	private final AdminErrorCode errorCode;

	public AdminException(AdminErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public AdminException(AdminErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public AdminErrorCode errorCode() {
		return errorCode;
	}
}
