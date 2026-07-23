package com.pikume.back.admin.application.dto;

public record AuthenticatedAdminSessionResult(
		String sessionId,
		String adminId,
		String role
) {
}
