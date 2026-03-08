package com.pikume.back.feed.application.readmodel;

import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FeedListItemView(
		Long diaryId,
		DiaryVisibility status,
		String content,
		List<String> imageUrls,
		LocalDate date,
		String nickname,
		String avatarPath,
		String userId,
		LocalDateTime createdAt,
		FriendStatus friendStatus,
		long commentCount,
		long likeCount,
		boolean liked
) {
}
