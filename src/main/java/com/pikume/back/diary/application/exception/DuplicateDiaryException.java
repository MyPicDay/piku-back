package com.pikume.back.diary.application.exception;

import java.time.LocalDate;

public class DuplicateDiaryException extends DiaryException {
	public DuplicateDiaryException(LocalDate date) {
		super(DiaryErrorCode.DUPLICATE_DIARY, DiaryErrorCode.DUPLICATE_DIARY.format(date));
	}
}
