package com.pikume.back.diary.exception;

import com.pikume.back.global.error.ErrorCode;
import com.pikume.back.global.exception.BusinessException;

public class DiaryAccessDeniedException extends BusinessException {
	public DiaryAccessDeniedException() {
		super(ErrorCode.DIARY_ACCESS_DENIED);
	}
}
