package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;

import java.time.LocalDateTime;

public record AdminSessionCacheEntry(
		String sessionId,
		String adminId,
		AdminSessionPhase phase,
		String sessionTokenHash,
		String csrfTokenHash,
		long authenticationVersion,
		LocalDateTime absoluteExpiresAt,
		LocalDateTime idleExpiresAt
) {

	public static AdminSessionCacheEntry from(AdminSession session) {
		return new AdminSessionCacheEntry(
				session.getId(),
				session.getAdminId(),
				session.getPhase(),
				session.getSessionTokenHash(),
				session.getCsrfTokenHash(),
				session.getAuthenticationVersion(),
				session.getAbsoluteExpiresAt(),
				session.getIdleExpiresAt());
	}

	public boolean isAuthenticatedAndActiveAt(LocalDateTime now) {
		return phase == AdminSessionPhase.AUTHENTICATED
				&& now.isBefore(absoluteExpiresAt)
				&& now.isBefore(idleExpiresAt);
	}
}
