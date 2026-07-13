package com.pikume.back.user.domain.exception;

public class InvalidEmailException extends IllegalArgumentException {

	public InvalidEmailException(String message) {
		super(message);
	}
}
