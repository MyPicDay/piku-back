package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;

import java.time.LocalDateTime;

public record CreateAdminAccountResult(
		String nickname,
		AdminRole role,
		AdminAccountStatus status,
		String temporaryPassword,
		LocalDateTime temporaryCredentialExpiresAt,
		boolean guideEmailSent
) {
}
