package com.pikume.back.diary.application.dto;

import java.time.LocalDate;

public record CalendarDiaryView(
		Long diaryId,
		String coverPhotoUrl,
		LocalDate date
) {
}
