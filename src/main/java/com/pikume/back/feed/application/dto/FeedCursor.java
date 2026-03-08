package com.pikume.back.feed.application.dto;

import java.time.LocalDateTime;

public record FeedCursor(
		FeedBucket bucket,
		long likeCount,
		long commentCount,
		LocalDateTime createdAt,
		long diaryId
) {
}
