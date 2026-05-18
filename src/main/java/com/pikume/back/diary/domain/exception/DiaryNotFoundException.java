package com.pikume.back.diary.domain.exception;

import com.pikume.back.global.error.ErrorCode;

public class DiaryNotFoundException extends RuntimeException {
	public DiaryNotFoundException() {
		super(ErrorCode.DIARY_NOT_FOUND.getMessage());
	}
}
