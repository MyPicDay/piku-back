package com.pikume.back.diary.domain.exception;

public class DuplicateDiaryException extends RuntimeException {
	public DuplicateDiaryException(String message) {
		super(message);
	}
}
