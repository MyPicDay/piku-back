package com.pikume.back.diary.exception;

import com.pikume.back.global.error.ErrorCode;
import com.pikume.back.global.exception.BusinessException;

public class DiaryNotFoundException extends BusinessException {
    public DiaryNotFoundException() {
        super(ErrorCode.DIARY_NOT_FOUND);
    }
}
