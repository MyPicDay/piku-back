package com.pikume.back.feed.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "피드 일기 조회 응답")
public record FeedDiaryResponse(
		Long diaryId,
		@Schema(description = "일기 공개범위") FeedVisibility status,
		String content,
		@Schema(description = "일기 사진") List<String> imgUrls,
		LocalDate date,
		String nickname,
		@Schema(description = "사용자 프로필 사진") String avatar,
		String userId,
		LocalDateTime createdAt,
		FeedFriendStatus friendStatus,
		Long commentCount,
		@Schema(description = "좋아요 수") Long likeCount,
		@Schema(description = "현재 사용자의 좋아요 여부") Boolean isLiked,
		@Schema(description = "현재 사용자의 일기 작성자 여부") Boolean isOwner
) {
}
