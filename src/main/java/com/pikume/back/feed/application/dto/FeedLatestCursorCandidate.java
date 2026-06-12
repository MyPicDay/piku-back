package com.pikume.back.feed.application.dto;

import java.time.LocalDate;

public record FeedLatestCursorCandidate(
		long diaryId,
		LocalDate date
) {

	public FeedCursor toCursor() {
		return FeedCursor.latest(date, diaryId);
	}
}
