package com.pikume.back.social.application.dto;

public record LikeResult(
		Long diaryId,
		long likeCount,
		boolean liked) {
}
