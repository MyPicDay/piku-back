package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record CreateAdminAccountCommand(
		String actorAdminId,
		String email,
		String nickname,
		AdminRole role
) {
}
