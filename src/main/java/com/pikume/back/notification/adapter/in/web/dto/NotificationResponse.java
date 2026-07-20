package com.pikume.back.notification.adapter.in.web.dto;

import com.pikume.back.notification.application.dto.NotificationKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "NotificationResponse")
public record NotificationResponse(
		@Schema(description = "알림 ID")
		Long id,
		@Schema(description = "알림 메시지")
		String message,
		@Schema(description = "보낸 사람 닉네임", nullable = true)
		String nickname,
		@Schema(description = "보낸 사람 프로필 사진", nullable = true)
		String avatarUrl,
		@Schema(description = "알림 유형")
		NotificationKind type,
		@Schema(description = "연관 Diary ID", nullable = true)
		Long relatedDiaryId,
		@Schema(description = "Diary 대표 사진", nullable = true)
		String thumbnailUrl,
		@Schema(description = "읽음 여부")
		Boolean isRead,
		@Schema(description = "생성 시각")
		LocalDateTime createdAt,
		@Schema(description = "Diary 날짜", nullable = true)
		LocalDate diaryDate,
		@Schema(description = "Diary 작성자 ID", nullable = true)
		String diaryUserId
) {
}
