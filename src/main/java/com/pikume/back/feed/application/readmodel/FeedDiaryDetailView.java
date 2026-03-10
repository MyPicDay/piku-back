package com.pikume.back.feed.application.readmodel;

import com.pikume.back.feed.application.dto.FeedVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FeedDiaryDetailView(
		Long diaryId,
		String userId,
		FeedVisibility status,
		String content,
		List<String> imageUrls,
		LocalDate date,
		LocalDateTime createdAt
) {
}
