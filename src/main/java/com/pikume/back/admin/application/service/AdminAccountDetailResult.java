package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;

import java.time.LocalDateTime;

public record AdminAccountDetailResult(
		String adminId,
		String email,
		String loginId,
		String nickname,
		AdminRole role,
		AdminAccountStatus status,
		boolean passwordChangeRequired,
		boolean otpRegistered,
		LocalDateTime temporaryCredentialExpiresAt,
		LocalDateTime lastLoginAt
) {

	public static AdminAccountDetailResult from(AdminAccount admin) {
		return new AdminAccountDetailResult(
				admin.getId(),
				admin.getEmail(),
				admin.getLoginId(),
				admin.getNickname(),
				admin.getRole(),
				admin.getStatus(),
				admin.isPasswordChangeRequired(),
				admin.isOtpRegistered(),
				admin.getTemporaryCredentialExpiresAt(),
				admin.getLastLoginAt());
	}
}
