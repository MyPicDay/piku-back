package com.pikume.back.feed.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cursor 기반 피드 페이지 응답")
public record FeedCursorPageResponse(
		List<FeedDiaryResponse> items,
		String nextCursor,
		boolean hasNext
) {
}
