package com.pikume.back.feed.application.dto;

import java.time.LocalDateTime;

public record FeedLatestCursorCandidate(
		long diaryId,
		LocalDateTime createdAt
) {

	public FeedCursor toCursor() {
		return FeedCursor.latest(createdAt, diaryId);
	}
}
