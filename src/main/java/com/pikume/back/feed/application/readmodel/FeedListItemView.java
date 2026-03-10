package com.pikume.back.feed.application.readmodel;

import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FeedListItemView(
		Long diaryId,
		FeedVisibility status,
		String content,
		List<String> imageUrls,
		LocalDate date,
		String nickname,
		String avatarPath,
		String userId,
		LocalDateTime createdAt,
		FeedFriendStatus friendStatus,
		long commentCount,
		long likeCount,
		boolean liked
) {
}
