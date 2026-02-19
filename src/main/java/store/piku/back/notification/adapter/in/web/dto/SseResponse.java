package store.piku.back.notification.adapter.in.web.dto;

import store.piku.back.notification.domain.vo.NotificationType;

public record SseResponse(
		NotificationType type,
		String message,
		Long diaryId,
		String senderId,
		String senderNickname,
		String senderAvatarUrl,
		String thumbnailUrl) {
}
