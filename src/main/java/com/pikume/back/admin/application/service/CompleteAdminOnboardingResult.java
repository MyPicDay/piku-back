package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record CompleteAdminOnboardingResult(
		String accessToken,
		long expiresInSeconds,
		String loginId,
		String nickname,
		String email,
		AdminRole role
) {
}
