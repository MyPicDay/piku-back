package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record AdminTokenIssueResult(
		String accessToken,
		String refreshToken,
		long accessTokenExpiresInSeconds,
		long refreshTokenExpiresInSeconds,
		String sessionId,
		String loginId,
		String nickname,
		String email,
		AdminRole role
) {
}
