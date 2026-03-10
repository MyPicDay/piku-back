package com.pikume.back.feed.application.readmodel;

import java.time.LocalDateTime;

public record FeedDiaryCandidateView(
		Long diaryId,
		String userId,
		LocalDateTime createdAt
) {
}
