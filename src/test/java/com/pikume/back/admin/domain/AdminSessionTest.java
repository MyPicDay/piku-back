package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdminSession")
class AdminSessionTest {

	@Nested
	@DisplayName("사전 세션")
	class PreAuthenticationSession {

		@Test
		@DisplayName("세션과 CSRF 원문의 해시만 저장한다")
		void storesOnlyCredentialHashes() {
			LocalDateTime now = LocalDateTime.now();

			AdminSession session = AdminSession.startAnonymous(
					"session-hash",
					"csrf-hash",
					now,
					now.plusMinutes(10));

			assertThat(session.getSessionTokenHash()).isEqualTo("session-hash");
			assertThat(session.getCsrfTokenHash()).isEqualTo("csrf-hash");
			assertThat(session.getAdminId()).isNull();
			assertThat(session.getPhase()).isEqualTo(AdminSessionPhase.ANONYMOUS);
			assertThat(session.getStatus()).isEqualTo(AdminSessionStatus.ACTIVE);
		}

		@Test
		@DisplayName("현재 단계와 일치할 때만 다음 인증 단계로 이동한다")
		void advancesOnlyFromExpectedPhase() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = anonymous(now);
			session.bindAdmin(
					"admin-1",
					3L,
					AdminSessionPhase.ONBOARDING_SET_LOGIN_ID,
					now.plusMinutes(10),
					now);

			session.advance(
					AdminSessionPhase.ONBOARDING_SET_LOGIN_ID,
					AdminSessionPhase.ONBOARDING_SET_PASSWORD,
					now.plusSeconds(1));

			assertThat(session.getPhase()).isEqualTo(AdminSessionPhase.ONBOARDING_SET_PASSWORD);
			assertThatThrownBy(() -> session.advance(
					AdminSessionPhase.ONBOARDING_SET_LOGIN_ID,
					AdminSessionPhase.ONBOARDING_REGISTER_OTP,
					now.plusSeconds(2)))
					.isInstanceOf(AdminDomainException.class);
		}
	}

	@Nested
	@DisplayName("인증 완료 세션")
	class AuthenticatedSession {

		@Test
		@DisplayName("인증 승격 시 세션과 CSRF 해시를 교체하고 인증 버전을 고정한다")
		void rotatesCredentialsWhenAuthenticated() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = anonymous(now);
			session.bindAdmin(
					"admin-1",
					4L,
					AdminSessionPhase.LOGIN_VERIFY_OTP,
					now.plusMinutes(5),
					now);

			session.authenticate(
					"new-session-hash",
					"new-csrf-hash",
					4L,
					now.plusHours(8),
					now.plusMinutes(30),
					now.plusSeconds(1));

			assertThat(session.getSessionTokenHash()).isEqualTo("new-session-hash");
			assertThat(session.getCsrfTokenHash()).isEqualTo("new-csrf-hash");
			assertThat(session.getPhase()).isEqualTo(AdminSessionPhase.AUTHENTICATED);
			assertThat(session.getAuthenticationVersion()).isEqualTo(4L);
			assertThat(session.isActiveAt(now.plusMinutes(29))).isTrue();
			assertThat(session.isActiveAt(now.plusMinutes(31))).isFalse();
		}

		@Test
		@DisplayName("마지막 활동 시각과 유휴 만료 시각을 갱신한다")
		void touchesActiveSession() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = authenticated(now);

			session.touch(now.plusMinutes(10), now.plusMinutes(40));

			assertThat(session.getLastActivityAt()).isEqualTo(now.plusMinutes(10));
			assertThat(session.getIdleExpiresAt()).isEqualTo(now.plusMinutes(40));
		}

		@Test
		@DisplayName("폐기된 세션은 다시 사용할 수 없다")
		void revokedSessionIsInactive() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = authenticated(now);

			session.revoke(now.plusMinutes(1));

			assertThat(session.getStatus()).isEqualTo(AdminSessionStatus.REVOKED);
			assertThat(session.isActiveAt(now.plusMinutes(2))).isFalse();
		}
	}

	private AdminSession anonymous(LocalDateTime now) {
		return AdminSession.startAnonymous("session-hash", "csrf-hash", now, now.plusMinutes(10));
	}

	private AdminSession authenticated(LocalDateTime now) {
		AdminSession session = anonymous(now);
		session.bindAdmin(
				"admin-1",
				2L,
				AdminSessionPhase.LOGIN_VERIFY_OTP,
				now.plusMinutes(5),
				now);
		session.authenticate(
				"new-session-hash",
				"new-csrf-hash",
				2L,
				now.plusHours(8),
				now.plusMinutes(30),
				now);
		return session;
	}
}
