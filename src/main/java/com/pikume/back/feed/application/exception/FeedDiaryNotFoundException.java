package com.pikume.back.feed.application.exception;

public class FeedDiaryNotFoundException extends FeedException {
	public FeedDiaryNotFoundException() {
		super(FeedErrorCode.DIARY_NOT_FOUND);
	}
}
