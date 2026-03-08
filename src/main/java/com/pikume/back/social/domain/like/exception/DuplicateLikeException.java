package com.pikume.back.social.domain.like.exception;

public class DuplicateLikeException extends RuntimeException {

	public DuplicateLikeException(String message, Throwable cause) {
		super(message, cause);
	}
}
