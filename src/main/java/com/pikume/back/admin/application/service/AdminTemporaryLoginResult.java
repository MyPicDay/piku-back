package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record AdminTemporaryLoginResult(
		String nextStep,
		String email,
		String nickname,
		AdminRole role
) {
}
