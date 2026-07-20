package com.pikume.back.notification.application.readmodel;

import java.time.LocalDate;

public record NotificationDiaryContextView(
		Long diaryId,
		String thumbnailUrl,
		LocalDate diaryDate,
		String diaryUserId,
		boolean anonymous
) {
}
