package com.pikume.back.diary.application.exception;

public class DiaryNotFoundException extends DiaryException {
	public DiaryNotFoundException() {
		super(DiaryErrorCode.DIARY_NOT_FOUND);
	}
}
