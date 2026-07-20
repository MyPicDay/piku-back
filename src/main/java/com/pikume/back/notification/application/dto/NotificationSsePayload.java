package com.pikume.back.notification.application.dto;

public record NotificationSsePayload(
		NotificationKind type,
		String message,
		Long diaryId,
		String senderId,
		String senderNickname,
		String senderAvatarUrl,
		String thumbnailUrl) {
}
