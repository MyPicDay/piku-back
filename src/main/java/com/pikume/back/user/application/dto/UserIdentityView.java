package com.pikume.back.user.application.dto;

public record UserIdentityView(
		String id,
		String passwordHash,
		String nickname,
		String avatarPath
) {
}
