package com.pikume.back.notification.application.dto;

import com.pikume.back.notification.domain.vo.NotificationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationResult(
		Long id,
		String message,
		String nickname,
		String avatarUrl,
		NotificationType type,
		Long relatedDiaryId,
		String thumbnailUrl,
		Boolean isRead,
		LocalDateTime createdAt,
		LocalDate diaryDate,
		String diaryUserId) {
}
