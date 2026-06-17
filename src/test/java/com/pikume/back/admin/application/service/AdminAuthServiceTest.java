package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminRefreshTokenClaims;
import com.pikume.back.admin.application.port.out.AdminTokenPort;
import com.pikume.back.admin.application.port.out.HashAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.LoadAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRefreshToken;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthService")
class AdminAuthServiceTest {

	@Mock
	private LoadAdminAccountPort loadAdminAccountPort;
	@Mock
	private LoadAdminSessionPort loadAdminSessionPort;
	@Mock
	private LoadAdminRefreshTokenPort loadAdminRefreshTokenPort;
	@Mock
	private HashAdminRefreshTokenPort hashAdminRefreshTokenPort;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AdminTokenPort adminTokenPort;
	@Mock
	private AdminOtpPort adminOtpPort;
	@Mock
	private ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	@Mock
	private AdminSessionTokenService adminSessionTokenService;

	@Test
	@DisplayName("정식 로그인 아이디와 패스워드가 유효하면 OTP challenge token을 발급한다")
	void loginReturnsOtpChallengeToken() {
		AdminAccount admin = readyAdmin();
		given(loadAdminAccountPort.findByLoginId("ops-june")).willReturn(Optional.of(admin));
		given(passwordEncoder.matches("AdminPass1!", "encoded-password")).willReturn(true);
		given(adminTokenPort.generateOtpChallengeToken(admin.getId())).willReturn("otp-challenge-token");
		given(adminTokenPort.otpChallengeTokenTtl()).willReturn(Duration.ofMinutes(5));

		AdminLoginChallengeResult result = service().login("ops-june", "AdminPass1!");

		assertThat(result.otpChallengeToken()).isEqualTo("otp-challenge-token");
		assertThat(result.expiresInSeconds()).isEqualTo(Duration.ofMinutes(5).toSeconds());
		assertThat(result.nextStep()).isEqualTo(AdminAuthStep.VERIFY_OTP.name());
	}

	@Test
	@DisplayName("패스워드 실패 5회 시 관리자 계정을 잠근다")
	void loginLocksAfterFivePasswordFailures() {
		AdminAccount admin = readyAdmin();
		given(loadAdminAccountPort.findByLoginId("ops-june")).willReturn(Optional.of(admin));
		given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);
		AdminAuthService service = service();

		for (int i = 0; i < 4; i++) {
			assertThatThrownBy(() -> service.login("ops-june", "wrong"))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.problem()).isEqualTo(AdminProblem.INVALID_CREDENTIALS));
		}

		assertThatThrownBy(() -> service.login("ops-june", "wrong"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.ACCOUNT_LOCKED));
		assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.LOCKED);
	}

	@Test
	@DisplayName("OTP 인증 성공 시 관리자 세션 토큰을 발급한다")
	void verifyOtpIssuesSessionTokens() {
		AdminAccount admin = readyAdmin();
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("SECRET");
		given(adminOtpPort.verify("SECRET", "123456")).willReturn(true);
		given(adminSessionTokenService.issueNewSession(eq(admin), any(LocalDateTime.class))).willReturn(tokenResult());

		AdminTokenIssueResult result = service().verifyOtp(admin.getId(), "123456");

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(admin.getOtpFailureCount()).isZero();
	}

	@Test
	@DisplayName("OTP 인증 5회 실패 후 10분 동안 OTP 인증을 차단한다")
	void verifyOtpBlocksAfterFiveFailures() {
		AdminAccount admin = readyAdmin();
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("SECRET");
		given(adminOtpPort.verify("SECRET", "000000")).willReturn(false);
		AdminAuthService service = service();

		for (int i = 0; i < 4; i++) {
			assertThatThrownBy(() -> service.verifyOtp(admin.getId(), "000000"))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.problem()).isEqualTo(AdminProblem.OTP_VERIFICATION_FAILED));
		}

		assertThatThrownBy(() -> service.verifyOtp(admin.getId(), "000000"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.OTP_BLOCKED));
		assertThat(admin.getOtpFailureCount()).isEqualTo(5);
		assertThat(admin.isOtpBlockedAt(LocalDateTime.now())).isTrue();
	}

	@Test
	@DisplayName("관리자 Refresh Token 재발급 시 기존 토큰을 회전한다")
	void reissueRotatesRefreshToken() {
		AdminAccount admin = readyAdmin();
		AdminSession session = activeSession(admin.getId());
		AdminRefreshToken refreshToken = activeRefreshToken(admin.getId(), session.getId(), "old-hash");
		given(hashAdminRefreshTokenPort.hash("old-refresh")).willReturn("old-hash");
		given(loadAdminRefreshTokenPort.findByTokenHash("old-hash")).willReturn(Optional.of(refreshToken));
		given(loadAdminSessionPort.findById(session.getId())).willReturn(Optional.of(session));
		given(adminTokenPort.readValidRefreshToken("old-refresh"))
				.willReturn(Optional.of(new AdminRefreshTokenClaims(admin.getId(), session.getId())));
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(adminSessionTokenService.rotate(eq(admin), eq(session), eq(refreshToken), any(LocalDateTime.class)))
				.willReturn(tokenResult());

		AdminTokenIssueResult result = service().reissue("old-refresh");

		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		then(adminSessionTokenService).should()
				.rotate(eq(admin), eq(session), eq(refreshToken), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("이미 회전된 관리자 Refresh Token이 재사용되면 세션 재사용 탐지를 기록한다")
	void reissueDetectsReusedRefreshToken() {
		AdminAccount admin = readyAdmin();
		AdminSession session = activeSession(admin.getId());
		AdminRefreshToken refreshToken = activeRefreshToken(admin.getId(), session.getId(), "old-hash");
		refreshToken.rotate(LocalDateTime.now().minusMinutes(1));
		given(hashAdminRefreshTokenPort.hash("old-refresh")).willReturn("old-hash");
		given(loadAdminRefreshTokenPort.findByTokenHash("old-hash")).willReturn(Optional.of(refreshToken));
		given(loadAdminSessionPort.findById(session.getId())).willReturn(Optional.of(session));

		assertThatThrownBy(() -> service().reissue("old-refresh"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.REFRESH_TOKEN_REUSED));
		then(adminSessionTokenService).should()
				.markRefreshTokenReuse(eq(session), eq(refreshToken), any(LocalDateTime.class));
	}

	private AdminAuthService service() {
		return new AdminAuthService(
				loadAdminAccountPort,
				loadAdminSessionPort,
				loadAdminRefreshTokenPort,
				hashAdminRefreshTokenPort,
				passwordEncoder,
				adminTokenPort,
				adminOtpPort,
				protectAdminOtpSecretPort,
				adminSessionTokenService);
	}

	private AdminAccount readyAdmin() {
		AdminAccount admin = AdminAccount.invite(
				"operator@pikume.com",
				"운영자1",
				AdminRole.OPERATOR,
				"temp-hash",
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().minusHours(1));
		admin.setLoginId("ops-june");
		admin.completePasswordSetup("encoded-password");
		admin.startOtpRegistration("protected-secret");
		admin.completeOtpRegistration();
		return admin;
	}

	private AdminSession activeSession(String adminId) {
		LocalDateTime now = LocalDateTime.now();
		return AdminSession.start(
				"session-1",
				adminId,
				"old-hash",
				now.plusHours(8),
				now.plusMinutes(30),
				now);
	}

	private AdminRefreshToken activeRefreshToken(String adminId, String sessionId, String tokenHash) {
		LocalDateTime now = LocalDateTime.now();
		return AdminRefreshToken.issue(tokenHash, sessionId, adminId, now.minusMinutes(1), now.plusMinutes(30));
	}

	private AdminTokenIssueResult tokenResult() {
		return new AdminTokenIssueResult(
				"access-token",
				"refresh-token",
				600L,
				1800L,
				"session-1",
				"ops-june",
				"운영자1",
				"operator@pikume.com",
				AdminRole.OPERATOR);
	}
}
