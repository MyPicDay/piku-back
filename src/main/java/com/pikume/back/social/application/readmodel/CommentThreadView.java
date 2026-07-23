package com.pikume.back.social.application.readmodel;

import java.time.LocalDateTime;

public record CommentThreadView(
		Long commentId,
		Long diaryId,
		String authorUserId,
		String content,
		Long parentCommentId,
		int replyCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		boolean deleted) {
}
