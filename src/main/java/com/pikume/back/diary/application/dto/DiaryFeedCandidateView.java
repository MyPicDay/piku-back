package com.pikume.back.diary.application.dto;

import java.time.LocalDateTime;

public record DiaryFeedCandidateView(
		Long diaryId,
		LocalDateTime createdAt
) {
}
