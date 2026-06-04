package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;

public record DiaryGalleryRow(
		Long diaryId,
		String coverPhotoPath,
		LocalDate date,
		Long imageCount,
		DiaryVisibility status
) {
}
