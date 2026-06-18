package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record AdminLoginChallengeResult(
		String nextStep,
		String loginId,
		String nickname,
		String email,
		AdminRole role
) {
}
