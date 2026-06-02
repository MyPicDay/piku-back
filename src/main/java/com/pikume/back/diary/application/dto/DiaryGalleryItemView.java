package com.pikume.back.diary.application.dto;

import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;

public record DiaryGalleryItemView(
		Long diaryId,
		String coverPhotoUrl,
		LocalDate date,
		Long imageCount,
		DiaryVisibility status
) {
}
