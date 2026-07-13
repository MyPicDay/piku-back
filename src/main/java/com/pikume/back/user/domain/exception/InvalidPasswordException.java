package com.pikume.back.user.domain.exception;

public class InvalidPasswordException extends IllegalArgumentException {

	public InvalidPasswordException(String message) {
		super(message);
	}
}
