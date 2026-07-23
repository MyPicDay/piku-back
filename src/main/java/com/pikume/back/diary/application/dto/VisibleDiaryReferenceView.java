package com.pikume.back.diary.application.dto;

public record VisibleDiaryReferenceView(
		Long diaryId,
		String ownerUserId,
		boolean anonymous) {
}
