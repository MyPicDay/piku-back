package com.pikume.back.user.application.dto;

public record UserIdentityView(
		String id,
		String email,
		String passwordHash,
		String nickname,
		String avatarPath
) {
}
