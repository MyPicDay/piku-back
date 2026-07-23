package com.pikume.back.character.application.exception;

import lombok.Getter;

@Getter
public class CharacterException extends RuntimeException {

	private final CharacterErrorCode errorCode;

	public CharacterException(CharacterErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}
