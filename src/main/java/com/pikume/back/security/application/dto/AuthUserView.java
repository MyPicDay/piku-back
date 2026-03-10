package com.pikume.back.security.application.dto;

public record AuthUserView(
		String id,
		String email,
		String password,
		String nickname,
		String avatarPath
) {
}
