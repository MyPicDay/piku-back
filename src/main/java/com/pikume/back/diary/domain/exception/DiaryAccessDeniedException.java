package com.pikume.back.diary.domain.exception;

import com.pikume.back.global.error.ErrorCode;

public class DiaryAccessDeniedException extends RuntimeException {
	public DiaryAccessDeniedException() {
		super(ErrorCode.DIARY_ACCESS_DENIED.getMessage());
	}
}
