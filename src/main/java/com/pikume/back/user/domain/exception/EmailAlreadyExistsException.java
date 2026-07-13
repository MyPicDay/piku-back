package com.pikume.back.user.domain.exception;

public class EmailAlreadyExistsException extends RuntimeException {
	public EmailAlreadyExistsException() {
		super("이미 가입된 이메일입니다.");
	}
}
