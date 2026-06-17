package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOnboardingService")
class AdminOnboardingServiceTest {

	@Mock
	private LoadAdminAccountPort loadAdminAccountPort;
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
	@DisplayName("이메일과 임시 패스워드가 유효하면 온보딩 토큰을 발급한다")
	void temporaryLoginReturnsOnboardingToken() {
		AdminAccount admin = invited();
		given(loadAdminAccountPort.findByEmail("operator@pikume.com")).willReturn(Optional.of(admin));
		given(passwordEncoder.matches("TempPass1!", "temp-hash")).willReturn(true);
		given(adminTokenPort.generateOnboardingToken(admin.getId())).willReturn("onboarding-token");
		given(adminTokenPort.onboardingTokenTtl()).willReturn(Duration.ofMinutes(10));

		AdminTemporaryLoginResult result = service().temporaryLogin("Operator@Pikume.com", "TempPass1!");

		assertThat(result.onboardingToken()).isEqualTo("onboarding-token");
		assertThat(result.nextStep()).isEqualTo(AdminOnboardingStep.SET_LOGIN_ID.name());
		assertThat(result.email()).isEqualTo("operator@pikume.com");
	}

	@Test
	@DisplayName("임시 패스워드가 틀리면 실패 횟수를 기록하고 예외를 던진다")
	void temporaryLoginRecordsFailureWhenPasswordMismatch() {
		AdminAccount admin = invited();
		given(loadAdminAccountPort.findByEmail("operator@pikume.com")).willReturn(Optional.of(admin));
		given(passwordEncoder.matches("wrong", "temp-hash")).willReturn(false);

		assertThatThrownBy(() -> service().temporaryLogin("operator@pikume.com", "wrong"))
				.isInstanceOf(AdminException.class);
		assertThat(admin.getLoginFailureCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("임시 로그인 패스워드 실패 5회 시 관리자 계정을 잠근다")
	void temporaryLoginLocksAfterFivePasswordFailures() {
		AdminAccount admin = invited();
		given(loadAdminAccountPort.findByEmail("operator@pikume.com")).willReturn(Optional.of(admin));
		given(passwordEncoder.matches("wrong", "temp-hash")).willReturn(false);
		AdminOnboardingService service = service();

		for (int i = 0; i < 4; i++) {
			assertThatThrownBy(() -> service.temporaryLogin("operator@pikume.com", "wrong"))
					.isInstanceOfSatisfying(AdminException.class, exception ->
							assertThat(exception.problem()).isEqualTo(AdminProblem.INVALID_CREDENTIALS));
		}

		assertThatThrownBy(() -> service.temporaryLogin("operator@pikume.com", "wrong"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.ACCOUNT_LOCKED));
		assertThat(admin.getStatus()).isEqualTo(AdminAccountStatus.LOCKED);
	}

	@Test
	@DisplayName("정식 로그인 아이디를 설정한다")
	void setLoginId() {
		AdminAccount admin = invited();
		given(loadAdminAccountPort.existsByLoginId("ops-june")).willReturn(false);
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));

		service().setLoginId(admin.getId(), "ops-june");

		assertThat(admin.getLoginId()).isEqualTo("ops-june");
	}

	@Test
	@DisplayName("정식 패스워드 설정 시 임시 패스워드를 무효화한다")
	void setPasswordInvalidatesTemporaryPassword() {
		AdminAccount admin = invited();
		admin.setLoginId("ops-june");
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(passwordEncoder.encode("NewAdmin1!")).willReturn("encoded-password");

		service().setPassword(admin.getId(), "NewAdmin1!");

		assertThat(admin.getPasswordHash()).isEqualTo("encoded-password");
		assertThat(admin.getTemporaryPasswordHash()).isNull();
		assertThat(admin.isPasswordChangeRequired()).isFalse();
	}

	@Test
	@DisplayName("OTP 등록 정보를 생성하고 보호된 비밀키를 저장한다")
	void startOtpRegistration() {
		AdminAccount admin = invited();
		admin.setLoginId("ops-june");
		admin.completePasswordSetup("encoded-password");
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(adminOtpPort.generateSecret()).willReturn("SECRET");
		given(protectAdminOtpSecretPort.protect("SECRET")).willReturn("protected-secret");
		given(adminOtpPort.provisioningUri("Pikume Ops", "ops-june", "SECRET"))
				.willReturn("otpauth://totp/Pikume");

		AdminOtpRegistrationResult result = service().startOtpRegistration(admin.getId());

		assertThat(result.manualEntryKey()).isEqualTo("SECRET");
		assertThat(result.provisioningUri()).isEqualTo("otpauth://totp/Pikume");
		assertThat(admin.getPendingOtpSecret()).isEqualTo("protected-secret");
	}

	@Test
	@DisplayName("OTP 인증이 성공하면 OTP 등록을 완료하고 관리자 Access Token을 발급한다")
	void verifyOtpCompletesOnboarding() {
		AdminAccount admin = invited();
		admin.setLoginId("ops-june");
		admin.completePasswordSetup("encoded-password");
		admin.startOtpRegistration("protected-secret");
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("SECRET");
		given(adminOtpPort.verify("SECRET", "123456")).willReturn(true);
		given(adminSessionTokenService.issueNewSession(eq(admin), any(LocalDateTime.class)))
				.willReturn(tokenResult());

		AdminTokenIssueResult result = service().verifyOtp(admin.getId(), "123456");

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(admin.isOtpRegistered()).isTrue();
		assertThat(admin.isOtpRegistrationRequired()).isFalse();
	}

	@Test
	@DisplayName("OTP 인증 5회 실패 후 10분 동안 OTP 인증을 차단한다")
	void verifyOtpBlocksAfterFiveFailures() {
		AdminAccount admin = invited();
		admin.setLoginId("ops-june");
		admin.completePasswordSetup("encoded-password");
		admin.startOtpRegistration("protected-secret");
		given(loadAdminAccountPort.findById(admin.getId())).willReturn(Optional.of(admin));
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("SECRET");
		given(adminOtpPort.verify("SECRET", "000000")).willReturn(false);
		AdminOnboardingService service = service();

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

	private AdminOnboardingService service() {
		return new AdminOnboardingService(
				loadAdminAccountPort,
				passwordEncoder,
				adminTokenPort,
				adminOtpPort,
				protectAdminOtpSecretPort,
				adminSessionTokenService);
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

	private AdminAccount invited() {
		return AdminAccount.invite(
				"operator@pikume.com",
				"운영자1",
				AdminRole.OPERATOR,
				"temp-hash",
				LocalDateTime.now().minusMinutes(1),
				LocalDateTime.now().plusHours(1));
	}
}
