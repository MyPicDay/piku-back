package com.pikume.back.admin.application.exception;

public class AdminAuthenticationStoreException extends RuntimeException {

	public AdminAuthenticationStoreException(String message) {
		super(message);
	}

	public AdminAuthenticationStoreException(String message, Throwable cause) {
		super(message, cause);
	}
}
