package com.pikume.back.feed.application.readmodel;

import com.pikume.back.feed.application.dto.FeedVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FeedDiaryItemSourceView(
		Long diaryId,
		String userId,
		FeedVisibility status,
		String content,
		List<FeedPhotoReferenceView> photos,
		LocalDate date,
		LocalDateTime createdAt
) {
}
