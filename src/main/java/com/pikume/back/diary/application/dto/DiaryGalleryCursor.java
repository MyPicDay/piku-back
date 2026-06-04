package com.pikume.back.diary.application.dto;

import java.time.LocalDate;

public record DiaryGalleryCursor(
		LocalDate date,
		Long diaryId
) {
}
