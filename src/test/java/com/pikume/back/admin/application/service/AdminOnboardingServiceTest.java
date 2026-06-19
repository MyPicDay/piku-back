package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.application.port.out.SaveAdminCredentialsPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminSessionPhase;
import com.pikume.back.admin.domain.exception.AdminDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOnboardingService")
class AdminOnboardingServiceTest {

	@Mock LoadAdminAccountPort loadAdminAccountPort;
	@Mock AdminPasswordPort adminPasswordPort;
	@Mock AdminOtpPort adminOtpPort;
	@Mock ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	@Mock com.pikume.back.admin.application.port.out.AdminSessionLifecyclePort adminSessionLifecyclePort;
	@Mock AdminSessionTelemetryPort telemetryPort;
	@Mock SaveAdminCredentialsPort saveAdminCredentialsPort;

	@Test
	@DisplayName("임시 로그인 성공은 사전 세션을 자격 증명 설정 단계에 결합한다")
	void temporaryLoginBindsPreAuthenticationSession() {
		AdminAccount admin = invitedAdmin();
		given(loadAdminAccountPort.findByEmail("operator@pikume.com")).willReturn(Optional.of(admin));
		given(adminPasswordPort.matches("TempPass1!", "temp-hash")).willReturn(true);

		AdminTemporaryLoginResult result = service().temporaryLogin(
				"raw-session", "operator@pikume.com", "TempPass1!");

		assertThat(result.nextStep()).isEqualTo(AdminOnboardingStep.SET_CREDENTIALS.name());
		then(adminSessionLifecyclePort).should().bindPreAuthentication(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(admin.getId()),
				org.mockito.ArgumentMatchers.eq(admin.getAuthenticationVersion()),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_SET_CREDENTIALS),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
		then(telemetryPort).should().loginSucceeded("onboarding", admin.getId());
	}

	@Test
	@DisplayName("임시 로그인 계정 저장소 장애는 인증 저장소 예외로 변환한다")
	void temporaryLoginStoreFailureIsServiceUnavailable() {
		given(loadAdminAccountPort.findByEmail("operator@pikume.com"))
				.willThrow(new DataAccessResourceFailureException("db unavailable"));

		assertThatThrownBy(() -> service().temporaryLogin(
				"raw-session", "operator@pikume.com", "TempPass1!"))
				.isInstanceOf(AdminAuthenticationStoreException.class);
	}

	@Test
	@DisplayName("잘못된 이메일 형식은 저장소 장애가 아니라 입력 오류로 유지한다")
	void invalidEmailRemainsDomainValidationFailure() {
		assertThatThrownBy(() -> service().temporaryLogin(
				"raw-session", "invalid-email", "TempPass1!"))
				.isInstanceOf(AdminDomainException.class);
	}

	@Test
	@DisplayName("로그인 아이디와 패스워드는 정확한 사전 세션 단계에서 함께 설정한다")
	void setCredentialsAdvancesExpectedPhase() {
		AdminAccount admin = invitedAdmin();
		given(adminSessionLifecyclePort.requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_SET_CREDENTIALS),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class))).willReturn(admin);
		given(adminPasswordPort.encode("Password1!")).willReturn("password-hash");
		given(saveAdminCredentialsPort.saveIfLoginIdAvailable(admin)).willReturn(true);

		service().setCredentials("raw-session", "ops-june", "Password1!");

		assertThat(admin.getLoginId()).isEqualTo("ops-june");
		assertThat(admin.getPasswordHash()).isEqualTo("password-hash");
		assertThat(admin.getTemporaryPasswordHash()).isNull();
		then(adminSessionLifecyclePort).should().advancePhase(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_SET_CREDENTIALS),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_REGISTER_OTP),
				org.mockito.ArgumentMatchers.eq(admin.getAuthenticationVersion()),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
	}

	@Test
	@DisplayName("DB 로그인 아이디 유니크 충돌은 409 문제로 변환한다")
	void databaseLoginIdConflictBecomesConflictProblem() {
		AdminAccount admin = invitedAdmin();
		given(adminSessionLifecyclePort.requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_SET_CREDENTIALS),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class))).willReturn(admin);
		given(adminPasswordPort.encode("Password1!")).willReturn("password-hash");
		given(saveAdminCredentialsPort.saveIfLoginIdAvailable(admin)).willReturn(false);

		assertThatThrownBy(() -> service().setCredentials("raw-session", "ops-june", "Password1!"))
				.isInstanceOfSatisfying(com.pikume.back.admin.application.exception.AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(
								com.pikume.back.admin.application.exception.AdminProblem.DUPLICATE_LOGIN_ID));

		then(adminSessionLifecyclePort).should(org.mockito.Mockito.never()).advancePhase(
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
	}

	@Test
	@DisplayName("패스워드 정책 위반 시 로그인 아이디도 저장하지 않는다")
	void invalidPasswordDoesNotPartiallySetLoginId() {
		AdminAccount admin = invitedAdmin();
		given(adminSessionLifecyclePort.requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_SET_CREDENTIALS),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class))).willReturn(admin);

		assertThatThrownBy(() -> service().setCredentials("raw-session", "ops-june", "weak"))
				.isInstanceOf(com.pikume.back.admin.application.exception.AdminException.class);

		assertThat(admin.getLoginId()).isNull();
		assertThat(admin.getPasswordHash()).isNull();
		assertThat(admin.getTemporaryPasswordHash()).isEqualTo("temp-hash");
		then(adminPasswordPort).shouldHaveNoInteractions();
		then(adminSessionLifecyclePort).should(org.mockito.Mockito.never()).advancePhase(
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
	}

	@Test
	@DisplayName("최초 OTP 성공은 온보딩 세션을 인증 완료 세션으로 교체한다")
	void verifyOtpCompletesOnboardingSession() {
		AdminAccount admin = invitedAdmin();
		admin.completeCredentialSetup("ops-june", "password-hash");
		admin.startOtpRegistration("protected-secret");
		given(adminSessionLifecyclePort.requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class))).willReturn(admin);
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("plain-secret");
		given(adminOtpPort.verify("plain-secret", "123456")).willReturn(true);
		given(adminSessionLifecyclePort.completeAuthentication(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.same(admin),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
				.willReturn(new AdminSessionCredentials("new-session", "new-csrf"));

		AdminAuthenticationResult result = service().verifyOtp("raw-session", "123456");

		assertThat(result.credentials()).isEqualTo(new AdminSessionCredentials("new-session", "new-csrf"));
		assertThat(admin.isOtpRegistered()).isTrue();
		then(adminSessionLifecyclePort).should().requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.ONBOARDING_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
		then(telemetryPort).should().otpSucceeded("onboarding", admin.getId());
	}

	private AdminOnboardingService service() {
		return new AdminOnboardingService(loadAdminAccountPort, adminPasswordPort, adminOtpPort,
				protectAdminOtpSecretPort, adminSessionLifecyclePort, telemetryPort, saveAdminCredentialsPort);
	}

	private AdminAccount invitedAdmin() {
		return AdminAccount.invite(
				"operator@pikume.com", "운영자1", AdminRole.OPERATOR, "temp-hash",
				LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(23));
	}
}
