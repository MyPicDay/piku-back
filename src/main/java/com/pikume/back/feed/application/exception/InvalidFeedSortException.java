package com.pikume.back.feed.application.exception;

public class InvalidFeedSortException extends FeedException {

	public InvalidFeedSortException() {
		super(FeedErrorCode.INVALID_SORT);
	}
}
