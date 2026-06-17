package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;

import java.time.LocalDateTime;

public record AdminAccountSummaryResult(
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

	public static AdminAccountSummaryResult from(AdminAccount admin) {
		return new AdminAccountSummaryResult(
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
