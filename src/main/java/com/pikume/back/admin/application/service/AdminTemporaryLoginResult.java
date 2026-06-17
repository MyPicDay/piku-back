package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record AdminTemporaryLoginResult(
		String onboardingToken,
		long expiresInSeconds,
		String nextStep,
		String email,
		String nickname,
		AdminRole role
) {
}
