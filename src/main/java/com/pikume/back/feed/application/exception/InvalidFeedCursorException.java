package com.pikume.back.feed.application.exception;

public class InvalidFeedCursorException extends FeedException {

	public InvalidFeedCursorException() {
		super(FeedErrorCode.INVALID_CURSOR);
	}
}
