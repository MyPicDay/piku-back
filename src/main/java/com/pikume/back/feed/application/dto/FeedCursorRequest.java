package com.pikume.back.feed.application.dto;

public record FeedCursorRequest(
		String cursor,
		int limit,
		FeedSortMode sortMode
) {

	public FeedCursorRequest {
		if (sortMode == null) {
			sortMode = FeedSortMode.RECOMMENDED;
		}
	}

	public FeedCursorRequest(String cursor, int limit) {
		this(cursor, limit, FeedSortMode.RECOMMENDED);
	}
}
