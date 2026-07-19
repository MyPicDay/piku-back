package com.pikume.back.feed.adapter.in.web;

import org.springframework.stereotype.Component;
import com.pikume.back.feed.adapter.in.web.dto.FeedCursorPageResponse;
import com.pikume.back.feed.adapter.in.web.dto.FeedDiaryResponse;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedDiaryResult;

@Component
public class FeedResponseMapper {

	public FeedDiaryResponse mapDiary(FeedDiaryResult result) {
		return new FeedDiaryResponse(
				result.getDiaryId(),
				result.getStatus(),
				result.getContent(),
				result.getImgUrls(),
				result.getDate(),
				result.getNickname(),
				result.getAvatar(),
				result.getUserId(),
				result.getCreatedAt(),
				result.getFriendStatus(),
				result.getCommentCount(),
				result.getLikeCount(),
				result.getIsLiked(),
				result.getIsOwner());
	}

	public FeedCursorPageResponse mapPage(FeedCursorPage<FeedDiaryResult> result) {
		return new FeedCursorPageResponse(
				result.items().stream()
						.map(this::mapDiary)
						.toList(),
				result.nextCursor(),
				result.hasNext());
	}
}
