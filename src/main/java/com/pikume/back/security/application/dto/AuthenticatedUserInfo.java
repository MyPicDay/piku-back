package com.pikume.back.security.application.dto;

public record AuthenticatedUserInfo(
		String id,
		String email,
		String nickname,
		String avatarPath
) {
}
