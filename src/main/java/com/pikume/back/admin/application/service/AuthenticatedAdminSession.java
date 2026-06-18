package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record AuthenticatedAdminSession(
		String sessionId,
		String adminId,
		AdminRole role
) {
}
