package com.pikume.back.social.application.readmodel;

import java.time.LocalDateTime;

public record CommentListView(
		Long commentId,
		Long diaryId,
		String userId,
		String nickname,
		String avatarPath,
		String content,
		Long parentId,
		int replyCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		boolean deleted
) {
}
