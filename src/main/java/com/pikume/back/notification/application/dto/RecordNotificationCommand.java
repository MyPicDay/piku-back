package com.pikume.back.notification.application.dto;

public record RecordNotificationCommand(
		String receiverId,
		NotificationKind kind,
		String senderId,
		Long diaryId
) {
}
