package store.piku.back.diary.exception;

import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.exception.BusinessException;

public class DiaryAccessDeniedException extends BusinessException {
	public DiaryAccessDeniedException() {
		super(ErrorCode.DIARY_ACCESS_DENIED);
	}
}
