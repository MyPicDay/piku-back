package com.pikume.back.diary.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VisibleDiaryDetailView(
		Long diaryId,
		String userId,
		DiaryVisibilityScope status,
		String content,
		List<DiaryPhotoView> photos,
		LocalDate date,
		LocalDateTime createdAt
) {
}
