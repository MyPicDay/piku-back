package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiarySummaryView(
		Long diaryId,
		String userId,
		DiaryVisibility status,
		String content,
		LocalDate date,
		LocalDateTime createdAt
) {
}
