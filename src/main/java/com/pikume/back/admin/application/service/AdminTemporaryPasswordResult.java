package com.pikume.back.admin.application.service;

import java.time.LocalDateTime;

public record AdminTemporaryPasswordResult(
		String temporaryLoginId,
		String temporaryPassword,
		LocalDateTime temporaryCredentialExpiresAt,
		boolean guideEmailSent
) {
}
