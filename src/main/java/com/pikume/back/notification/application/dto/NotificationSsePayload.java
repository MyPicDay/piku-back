package com.pikume.back.notification.application.dto;

import com.pikume.back.notification.domain.vo.NotificationType;

public record NotificationSsePayload(
		NotificationType type,
		String message,
		Long diaryId,
		String senderId,
		String senderNickname,
		String senderAvatarUrl,
		String thumbnailUrl) {
}
