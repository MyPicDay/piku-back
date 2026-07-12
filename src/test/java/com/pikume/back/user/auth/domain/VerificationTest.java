package com.pikume.back.user.auth.domain;

import com.pikume.back.user.auth.domain.service.EmailVerificationPolicy;
import com.pikume.back.user.auth.domain.vo.VerificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verification")
class VerificationTest {

	@Test
	@DisplayName("인증 코드와 목적이 모두 일치해야 한다")
	void matchesCodeAndPurpose() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 12, 10, 0);
		Verification verification = new Verification(
				"user@example.com", "123456", VerificationType.SIGN_UP, now.plusMinutes(5));

		assertThat(verification.matches("123456", VerificationType.SIGN_UP)).isTrue();
		assertThat(verification.matches("999999", VerificationType.SIGN_UP)).isFalse();
		assertThat(verification.matches("123456", VerificationType.PASSWORD_RESET)).isFalse();
	}

	@Test
	@DisplayName("인증 코드 만료 시각은 이메일 검증 정책이 결정한다")
	void expirationIsOwnedByPolicy() {
		LocalDateTime issuedAt = LocalDateTime.of(2026, 7, 12, 10, 0);
		EmailVerificationPolicy policy = new EmailVerificationPolicy();

		assertThat(policy.codeExpiresAt(issuedAt)).isEqualTo(issuedAt.plusMinutes(5));
		assertThat(policy.isCodeExpired(issuedAt.plusMinutes(5), issuedAt.plusMinutes(6))).isTrue();
	}
}
