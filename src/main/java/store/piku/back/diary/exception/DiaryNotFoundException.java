package store.piku.back.diary.exception;

import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.exception.BusinessException;

public class DiaryNotFoundException extends BusinessException {
    public DiaryNotFoundException() {
        super(ErrorCode.DIARY_NOT_FOUND);
    }
}
