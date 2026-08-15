package com.pikume.back.user.application.dto;

public record UserSummaryView(
		String id,
		String nickname,
		UserAvatarReference avatarReference
) {
}
