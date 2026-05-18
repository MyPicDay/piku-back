package com.pikume.back.feed.domain.exception;

import com.pikume.back.global.error.ErrorCode;

public class InvalidFeedCursorException extends RuntimeException {

	public InvalidFeedCursorException() {
		super(ErrorCode.INVALID_FEED_CURSOR.getMessage());
	}
}
