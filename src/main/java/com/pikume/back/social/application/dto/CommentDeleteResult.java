package com.pikume.back.social.application.dto;

public record CommentDeleteResult(
		boolean success,
		String message,
		Long commentId) {
}
