package com.pikume.back.diary.application.dto;

import java.time.LocalDate;

public record DiaryFeedCandidateView(
		Long diaryId,
		LocalDate date
) {
}
