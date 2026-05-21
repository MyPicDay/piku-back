package com.pikume.back.diary.application.exception;

public class DiaryAccessDeniedException extends DiaryException {
	public DiaryAccessDeniedException() {
		super(DiaryErrorCode.DIARY_ACCESS_DENIED);
	}
}
