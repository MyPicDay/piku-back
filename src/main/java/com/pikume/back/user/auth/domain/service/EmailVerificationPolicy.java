package com.pikume.back.user.auth.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;

public class EmailVerificationPolicy {

	private static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
	private static final Duration COMPLETED_VERIFICATION_VALIDITY = Duration.ofMinutes(10);

	public LocalDateTime codeExpiresAt(LocalDateTime issuedAt) {
		return issuedAt.plus(CODE_VALIDITY);
	}

	public boolean isCodeExpired(LocalDateTime expiresAt, LocalDateTime checkedAt) {
		return expiresAt.isBefore(checkedAt);
	}

	public boolean isCompletedVerificationExpired(LocalDateTime verifiedAt, LocalDateTime checkedAt) {
		return verifiedAt.isBefore(checkedAt.minus(COMPLETED_VERIFICATION_VALIDITY));
	}
}
