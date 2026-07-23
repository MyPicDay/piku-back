package com.pikume.back.social.application.exception;

public class SocialException extends RuntimeException {

	private final SocialErrorCode errorCode;

	public SocialException(SocialErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public SocialException(SocialErrorCode errorCode, Throwable cause) {
		super(errorCode.message(), cause);
		this.errorCode = errorCode;
	}

	public SocialErrorCode getErrorCode() {
		return errorCode;
	}
}
