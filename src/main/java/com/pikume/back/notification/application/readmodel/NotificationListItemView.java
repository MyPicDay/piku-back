package com.pikume.back.notification.application.readmodel;

import com.pikume.back.notification.application.dto.NotificationKind;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationListItemView(
		Long id,
		String message,
		String nickname,
		String avatarUrl,
		NotificationKind kind,
		Long relatedDiaryId,
		String thumbnailUrl,
		Boolean isRead,
		LocalDateTime createdAt,
		LocalDate diaryDate,
		String diaryUserId
) {
}
