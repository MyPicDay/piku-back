package com.pikume.back.feed.application.exception;

import lombok.Getter;

@Getter
public class FeedException extends RuntimeException {

	private final FeedErrorCode errorCode;

	public FeedException(FeedErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
