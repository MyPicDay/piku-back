package com.pikume.back.feed.domain.exception;

import com.pikume.back.global.error.ErrorCode;
import com.pikume.back.global.exception.BusinessException;

public class InvalidFeedCursorException extends BusinessException {

	public InvalidFeedCursorException() {
		super(ErrorCode.INVALID_FEED_CURSOR);
	}
}
