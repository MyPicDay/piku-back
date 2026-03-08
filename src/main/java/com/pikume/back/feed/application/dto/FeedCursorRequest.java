package com.pikume.back.feed.application.dto;

public record FeedCursorRequest(
		String cursor,
		int limit
) {
}
