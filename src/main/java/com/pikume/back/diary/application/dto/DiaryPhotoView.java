package com.pikume.back.diary.application.dto;

public record DiaryPhotoView(
		Long diaryId,
		String path,
		boolean represent
) {
}
