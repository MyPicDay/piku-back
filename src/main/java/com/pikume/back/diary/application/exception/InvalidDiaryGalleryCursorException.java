package com.pikume.back.diary.application.exception;

public class InvalidDiaryGalleryCursorException extends RuntimeException {

	public InvalidDiaryGalleryCursorException() {
		super("유효하지 않은 갤러리 커서입니다.");
	}
}
