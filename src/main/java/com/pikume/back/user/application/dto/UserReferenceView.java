package com.pikume.back.user.application.dto;

public record UserReferenceView(
		String id,
		String nickname,
		UserAvatarReference avatarReference
) {
}
