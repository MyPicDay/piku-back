package com.pikume.back.social.application.dto;

import java.time.LocalDateTime;

public record CommentResult(
		Long id,
		String content,
		LocalDateTime createdAt) {
}
