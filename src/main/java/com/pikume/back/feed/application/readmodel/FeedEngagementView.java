package com.pikume.back.feed.application.readmodel;

public record FeedEngagementView(
		Long diaryId,
		long commentCount,
		long likeCount,
		boolean liked
) {
}
