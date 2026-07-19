package com.pikume.back.feed.application.readmodel;

public record FeedAuthorView(
		String userId,
		String nickname,
		String avatarUrl
) {
}
