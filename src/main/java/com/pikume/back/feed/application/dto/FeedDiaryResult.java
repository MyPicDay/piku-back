package com.pikume.back.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedDiaryResult {
	private Long diaryId;

	private FeedVisibility status;
	private String content;

	private List<String> imgUrls;
	private LocalDate date;
	private String nickname;

	private String avatar;

	private String userId;

	private LocalDateTime createdAt;

	private FeedFriendStatus friendStatus;

	private Long commentCount;

	private Long likeCount;

	private Boolean isLiked;

	private Boolean isOwner;
}
