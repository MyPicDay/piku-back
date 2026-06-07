package com.pikume.back.notification.application.readmodel;

import com.pikume.back.notification.domain.vo.NotificationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationListView(
		Long notificationId,
		String senderNickname,
		String senderAvatarPath,
		NotificationType type,
		Long diaryId,
		String thumbnailUrl,
		Boolean isRead,
		LocalDateTime createdAt,
		LocalDate diaryDate,
		String diaryUserId,
		boolean anonymousDiary
) {
}
