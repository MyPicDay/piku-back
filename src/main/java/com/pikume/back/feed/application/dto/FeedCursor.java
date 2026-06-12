package com.pikume.back.feed.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeedCursor(
		FeedSortMode sortMode,
		FeedBucket bucket,
		long likeCount,
		long commentCount,
		LocalDateTime createdAt,
		LocalDate date,
		long diaryId
) {

	public FeedCursor(FeedBucket bucket, long likeCount, long commentCount, LocalDateTime createdAt, long diaryId) {
		this(null, bucket, likeCount, commentCount, createdAt, null, diaryId);
	}

	public static FeedCursor recommended(FeedBucket bucket, long likeCount, long commentCount,
			LocalDateTime createdAt, long diaryId) {
		return new FeedCursor(FeedSortMode.RECOMMENDED, bucket, likeCount, commentCount, createdAt, null, diaryId);
	}

	public static FeedCursor latest(LocalDate date, long diaryId) {
		return new FeedCursor(FeedSortMode.LATEST, null, 0L, 0L, null, date, diaryId);
	}
}
