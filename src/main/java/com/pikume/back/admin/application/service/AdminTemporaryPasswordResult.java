package com.pikume.back.admin.application.service;

import java.time.LocalDateTime;

public record AdminTemporaryPasswordResult(
		String temporaryPassword,
		LocalDateTime temporaryCredentialExpiresAt,
		boolean guideEmailSent
) {
}
