package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import com.pikume.back.admin.application.dto.AuthenticatedAdminSessionResult;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.out.AdminSessionCacheEntry;
import com.pikume.back.admin.application.port.out.AdminSessionCachePort;
import com.pikume.back.admin.application.port.out.AdminSessionCredentialPort;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.QueryAdminSessionPort;
import com.pikume.back.admin.application.port.out.RecordAdminSessionPort;
import com.pikume.back.admin.application.port.out.TouchAdminSessionPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSessionService")
class AdminSessionServiceTest {

	@Mock
	private QueryAdminSessionPort queryAdminSessionPort;
	@Mock
	private RecordAdminSessionPort recordAdminSessionPort;
	@Mock
	private TouchAdminSessionPort touchAdminSessionPort;
	@Mock
	private AdminSessionCachePort adminSessionCachePort;
	@Mock
	private AdminSessionCredentialPort adminSessionCredentialPort;
	@Mock
	private QueryAdminAccountPort queryAdminAccountPort;
	@Mock
	private AdminSessionTelemetryPort telemetryPort;

	@Nested
	@DisplayName("사전 세션 발급")
	class Initialize {

		@Test
		@DisplayName("원문은 응답하고 해시만 DB 세션에 저장한다")
		void storesOnlyCredentialHashes() {
			given(adminSessionCredentialPort.generate())
					.willReturn("raw-session", "raw-csrf");
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCredentialPort.hash("raw-csrf")).willReturn("csrf-hash");
			given(recordAdminSessionPort.recordSession(any(AdminSession.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			AdminSessionCredentialResult result = service().initialize(LocalDateTime.now());

			assertThat(result.sessionToken()).isEqualTo("raw-session");
			assertThat(result.csrfToken()).isEqualTo("raw-csrf");
			then(recordAdminSessionPort).should().recordSession(any(AdminSession.class));
		}

	}

	@Nested
	@DisplayName("인증 세션 검증")
	class Authentication {

		@Test
		@DisplayName("Redis 캐시가 비어 있으면 DB 세션으로 인증하고 캐시를 채운다")
		void fallsBackToDatabaseOnCacheMiss() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSession session = authenticatedSession(admin, now);
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCachePort.findByTokenHash("session-hash")).willReturn(Optional.empty());
			given(queryAdminSessionPort.findSessionByTokenHash("session-hash")).willReturn(Optional.of(session));
			given(queryAdminAccountPort.findAccount(admin.getId())).willReturn(Optional.of(admin));
			given(touchAdminSessionPort.touchAuthenticated(
					"session-hash", admin.getAuthenticationVersion(), now, now.plusMinutes(30)))
					.willReturn(true);

			AuthenticatedAdminSessionResult result = service().authenticate("raw-session", now);

			assertThat(result.adminId()).isEqualTo(admin.getId());
			assertThat(result.role()).isEqualTo(AdminRole.OPERATOR.name());
			then(adminSessionCachePort).should().put(any(AdminSessionCacheEntry.class));
		}

		@Test
		@DisplayName("Redis 캐시의 인증 버전이 현재 관리자와 다르면 접근을 거부한다")
		void rejectsStaleCachedAuthenticationVersion() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSessionCacheEntry staleEntry = new AdminSessionCacheEntry(
					"session-1",
					admin.getId(),
					AdminSessionPhase.AUTHENTICATED,
					"session-hash",
					"csrf-hash",
					admin.getAuthenticationVersion() - 1,
					now.plusHours(8),
					now.plusMinutes(30));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCachePort.findByTokenHash("session-hash")).willReturn(Optional.of(staleEntry));
			given(queryAdminAccountPort.findAccount(admin.getId())).willReturn(Optional.of(admin));

			assertThatThrownBy(() -> service().authenticate("raw-session", now))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.UNAUTHENTICATED));
			then(adminSessionCachePort).should().evict("session-hash");
			then(touchAdminSessionPort).should(never())
					.touchAuthenticated(any(), any(Long.class), any(), any());
		}

		@Test
		@DisplayName("DB 세션 저장소를 조회할 수 없으면 Redis만으로 인증하지 않는다")
		void failsClosedWhenDatabaseIsUnavailable() {
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCachePort.findByTokenHash("session-hash")).willReturn(Optional.empty());
			given(queryAdminSessionPort.findSessionByTokenHash("session-hash"))
					.willThrow(new IllegalStateException("db unavailable"));

			assertThatThrownBy(() -> service().authenticate("raw-session", LocalDateTime.now()))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.SESSION_STORE_UNAVAILABLE));
		}

		@Test
		@DisplayName("Redis 연결 예외가 발생하면 DB 세션으로 인증을 계속한다")
		void fallsBackToDatabaseWhenRedisIsUnavailable() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSession session = authenticatedSession(admin, now);
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCachePort.findByTokenHash("session-hash"))
					.willThrow(new IllegalStateException("redis unavailable"));
			given(queryAdminSessionPort.findSessionByTokenHash("session-hash")).willReturn(Optional.of(session));
			given(queryAdminAccountPort.findAccount(admin.getId())).willReturn(Optional.of(admin));
			given(touchAdminSessionPort.touchAuthenticated(
					"session-hash", admin.getAuthenticationVersion(), now, now.plusMinutes(30)))
					.willReturn(true);

			AuthenticatedAdminSessionResult result = service().authenticate("raw-session", now);

			assertThat(result.adminId()).isEqualTo(admin.getId());
			then(telemetryPort).should().cacheOperationFailed("read");
			then(adminSessionCachePort).should().put(any(AdminSessionCacheEntry.class));
		}

		@Test
		@DisplayName("Redis 캐시가 남아 있어도 현재 관리자 DB 상태를 확인할 수 없으면 503으로 거부한다")
		void failsClosedWithCachedSessionWhenAdminDatabaseIsUnavailable() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSessionCacheEntry cached = AdminSessionCacheEntry.from(authenticatedSession(admin, now));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCachePort.findByTokenHash("session-hash")).willReturn(Optional.of(cached));
			given(queryAdminAccountPort.findAccount(admin.getId())).willThrow(new IllegalStateException("db unavailable"));

			assertThatThrownBy(() -> service().authenticate("raw-session", now))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.SESSION_STORE_UNAVAILABLE));
		}

		@Test
		@DisplayName("캐시가 유효해 보여도 DB 세션 갱신이 실패하면 폐기 세션으로 거부한다")
		void rejectsCachedSessionWhenDatabaseSessionWasRevoked() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSessionCacheEntry cached = AdminSessionCacheEntry.from(authenticatedSession(admin, now));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(adminSessionCachePort.findByTokenHash("session-hash")).willReturn(Optional.of(cached));
			given(queryAdminAccountPort.findAccount(admin.getId())).willReturn(Optional.of(admin));
			given(touchAdminSessionPort.touchAuthenticated(
					"session-hash", admin.getAuthenticationVersion(), now, now.plusMinutes(30)))
					.willReturn(false);

			assertThatThrownBy(() -> service().authenticate("raw-session", now))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.UNAUTHENTICATED));
			then(adminSessionCachePort).should().evict("session-hash");
		}
	}

	@Nested
	@DisplayName("세션 결합 CSRF 검증")
	class CsrfValidation {

		@Test
		@DisplayName("사전 세션에 저장된 CSRF 해시와 쿠키 원문이 일치하면 허용한다")
		void acceptsMatchingPreAuthenticationSession() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = AdminSession.startAnonymous(
					"session-hash", "csrf-hash", now.minusSeconds(1), now.plusMinutes(10));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(queryAdminSessionPort.findSessionByTokenHash("session-hash")).willReturn(Optional.of(session));
			given(adminSessionCredentialPort.matches("raw-csrf", "csrf-hash")).willReturn(true);

			service().validateCsrf("raw-session", "raw-csrf", now);

			then(adminSessionCredentialPort).should().matches("raw-csrf", "csrf-hash");
		}

		@Test
		@DisplayName("CSRF 원문이 서버 세션 해시와 다르면 접근을 거부한다")
		void rejectsMismatchedToken() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = AdminSession.startAnonymous(
					"session-hash", "csrf-hash", now.minusSeconds(1), now.plusMinutes(10));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(queryAdminSessionPort.findSessionByTokenHash("session-hash")).willReturn(Optional.of(session));
			given(adminSessionCredentialPort.matches("wrong-csrf", "csrf-hash")).willReturn(false);

			assertThatThrownBy(() -> service().validateCsrf("raw-session", "wrong-csrf", now))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.CSRF_INVALID));
		}
	}

	@Nested
	@DisplayName("인증 단계 상태화")
	class AuthenticationPhase {

		@Test
		@DisplayName("익명 세션을 관리자와 결합해 로그인 OTP 단계로 전환한다")
		void bindsAnonymousSessionToAdmin() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = AdminSession.startAnonymous(
					"session-hash", "csrf-hash", now.minusSeconds(1), now.plusMinutes(10));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(queryAdminSessionPort.lockSessionByTokenHash("session-hash"))
					.willReturn(Optional.of(session));
			given(recordAdminSessionPort.recordSession(session)).willReturn(session);

			service().bindPreAuthentication(
					"raw-session", "admin-1", 3L, AdminSessionPhase.LOGIN_VERIFY_OTP, now);

			assertThat(session.getAdminId()).isEqualTo("admin-1");
			assertThat(session.getPhase()).isEqualTo(AdminSessionPhase.LOGIN_VERIFY_OTP);
			then(telemetryPort).should().phaseChanged(
					session.getId(),
					"admin-1",
					AdminSessionPhase.ANONYMOUS,
					AdminSessionPhase.LOGIN_VERIFY_OTP,
					now);
		}

		@Test
		@DisplayName("사전 세션 단계 전환은 이전 단계와 다음 단계를 기록한다")
		void recordsPreAuthenticationPhaseTransition() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = AdminSession.startAnonymous(
					"session-hash", "csrf-hash", now.minusSeconds(1), now.plusMinutes(10));
			session.bindAdmin("admin-1", 2L, AdminSessionPhase.ONBOARDING_SET_CREDENTIALS,
					now.plusMinutes(10), now.minusSeconds(1));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(queryAdminSessionPort.findSessionByTokenHash("session-hash")).willReturn(Optional.of(session));

			service().advancePhase(
					"raw-session",
					AdminSessionPhase.ONBOARDING_SET_CREDENTIALS,
					AdminSessionPhase.ONBOARDING_REGISTER_OTP,
					3L,
					now);

			then(telemetryPort).should().phaseChanged(
					session.getId(),
					"admin-1",
					AdminSessionPhase.ONBOARDING_SET_CREDENTIALS,
					AdminSessionPhase.ONBOARDING_REGISTER_OTP,
					now);
		}

		@Test
		@DisplayName("현재 사전 세션 단계가 요청 단계와 다르면 거부한다")
		void rejectsUnexpectedPhase() {
			LocalDateTime now = LocalDateTime.now();
			AdminSession session = AdminSession.startAnonymous(
					"session-hash", "csrf-hash", now.minusSeconds(1), now.plusMinutes(10));
			session.bindAdmin("admin-1", 2L, AdminSessionPhase.ONBOARDING_REGISTER_OTP,
					now.plusMinutes(10), now.minusSeconds(1));
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(queryAdminSessionPort.lockSessionByTokenHash("session-hash"))
					.willReturn(Optional.of(session));

			assertThatThrownBy(() -> service().requirePhaseForUpdate(
					"raw-session", AdminSessionPhase.ONBOARDING_SET_CREDENTIALS, now))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.UNAUTHENTICATED));
		}

		@Test
		@DisplayName("계정 인증 버전이 변경된 사전 세션은 같은 단계여도 거부한다")
		void rejectsStalePreAuthenticationVersion() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSession session = AdminSession.startAnonymous(
					"session-hash", "csrf-hash", now.minusSeconds(1), now.plusMinutes(10));
			session.bindAdmin(admin.getId(), admin.getAuthenticationVersion(),
					AdminSessionPhase.LOGIN_VERIFY_OTP, now.plusMinutes(10), now.minusSeconds(1));
			admin.advanceAuthenticationVersion();
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("session-hash");
			given(queryAdminSessionPort.lockSessionByTokenHash("session-hash"))
					.willReturn(Optional.of(session));
			given(queryAdminAccountPort.lockAccount(admin.getId())).willReturn(Optional.of(admin));

			assertThatThrownBy(() -> service().requirePhaseForUpdate(
					"raw-session", AdminSessionPhase.LOGIN_VERIFY_OTP, now))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.UNAUTHENTICATED));
		}

		@Test
		@DisplayName("OTP 성공은 기존 인증 세션을 폐기하고 세션과 CSRF 자격 증명을 모두 교체한다")
		void rotatesCredentialsAndRevokesPreviousSession() {
			LocalDateTime now = LocalDateTime.now();
			AdminAccount admin = readyAdmin();
			AdminSession preSession = AdminSession.startAnonymous(
					"old-session-hash", "old-csrf-hash", now.minusMinutes(1), now.plusMinutes(5));
			preSession.bindAdmin(admin.getId(), admin.getAuthenticationVersion(),
					AdminSessionPhase.LOGIN_VERIFY_OTP, now.plusMinutes(5), now.minusSeconds(30));
			AdminSession previous = authenticatedSession(admin, now);
			given(adminSessionCredentialPort.hash("raw-session")).willReturn("old-session-hash");
			given(queryAdminSessionPort.findSessionByTokenHash("old-session-hash")).willReturn(Optional.of(preSession));
			given(queryAdminSessionPort.findActiveSessions(admin.getId())).willReturn(java.util.List.of(previous, preSession));
			given(adminSessionCredentialPort.generate()).willReturn("new-session", "new-csrf");
			given(adminSessionCredentialPort.hash("new-session")).willReturn("new-session-hash");
			given(adminSessionCredentialPort.hash("new-csrf")).willReturn("new-csrf-hash");

			AdminSessionCredentialResult credentials = service().completeAuthentication(
					"raw-session", admin, AdminSessionPhase.LOGIN_VERIFY_OTP, now);

			assertThat(credentials).isEqualTo(new AdminSessionCredentialResult("new-session", "new-csrf"));
			assertThat(previous.getStatus()).isEqualTo(com.pikume.back.admin.domain.AdminSessionStatus.REVOKED);
			assertThat(preSession.getPhase()).isEqualTo(AdminSessionPhase.AUTHENTICATED);
			assertThat(preSession.getSessionTokenHash()).isEqualTo("new-session-hash");
			then(recordAdminSessionPort).should().recordSession(preSession);
			then(adminSessionCachePort).should().evict("old-session-hash");
			then(telemetryPort).should().phaseChanged(
					preSession.getId(),
					admin.getId(),
					AdminSessionPhase.LOGIN_VERIFY_OTP,
					AdminSessionPhase.AUTHENTICATED,
					now);
		}
	}

	private AdminSessionService service() {
		return new AdminSessionService(
				queryAdminSessionPort,
				recordAdminSessionPort,
				touchAdminSessionPort,
				adminSessionCachePort,
				adminSessionCredentialPort,
				queryAdminAccountPort,
				telemetryPort);
	}

	private AdminAccount readyAdmin() {
		AdminAccount admin = AdminAccount.invite(
				"operator@pikume.com",
				"운영자1",
				AdminRole.OPERATOR,
				"temp-hash",
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1));
		admin.completeCredentialSetup("ops-june", "password-hash");
		admin.startOtpRegistration("protected-secret");
		admin.completeOtpRegistration();
		return admin;
	}

	private AdminSession authenticatedSession(AdminAccount admin, LocalDateTime now) {
		AdminSession session = AdminSession.startAnonymous(
				"session-hash", "csrf-hash", now.minusMinutes(1), now.plusMinutes(5));
		session.bindAdmin(
				admin.getId(),
				admin.getAuthenticationVersion(),
				AdminSessionPhase.LOGIN_VERIFY_OTP,
				now.plusMinutes(5),
				now.minusSeconds(30));
		session.authenticate(
				"session-hash",
				"csrf-hash",
				admin.getAuthenticationVersion(),
				now.plusHours(8),
				now.plusMinutes(30),
				now.minusSeconds(10));
		return session;
	}
}
