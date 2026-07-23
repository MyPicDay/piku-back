package com.pikume.back.diary.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiarySummaryView(
		Long diaryId,
		String userId,
		DiaryVisibilityScope status,
		String content,
		LocalDate date,
		LocalDateTime createdAt
) {
}
