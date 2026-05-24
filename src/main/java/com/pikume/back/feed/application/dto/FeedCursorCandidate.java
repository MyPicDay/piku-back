package com.pikume.back.feed.application.dto;

import java.time.LocalDateTime;

public record FeedCursorCandidate(
		FeedBucket bucket,
		long diaryId,
		long likeCount,
		long commentCount,
		LocalDateTime createdAt
) {

	public FeedCursor toCursor() {
		return FeedCursor.recommended(bucket, likeCount, commentCount, createdAt, diaryId);
	}
}
