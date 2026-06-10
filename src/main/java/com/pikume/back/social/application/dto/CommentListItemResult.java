package com.pikume.back.social.application.dto;

import java.time.LocalDateTime;

public record CommentListItemResult(
		Long id,
		Long diaryId,
		String userId,
		String nickname,
		String avatar,
		String content,
		Long parentId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		int replyCount,
		boolean canReply,
		boolean canEdit,
		boolean canDelete) {
}
