package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
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
@DisplayName("AdminAuthService")
class AdminAuthServiceTest {

	@Mock QueryAdminAccountPort queryAdminAccountPort;
	@Mock AdminPasswordPort adminPasswordPort;
	@Mock AdminOtpPort adminOtpPort;
	@Mock ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	@Mock com.pikume.back.admin.application.port.in.ManageAdminSessionLifecycleUseCase adminSessionLifecyclePort;
	@Mock AdminSessionTelemetryPort telemetryPort;

	@Test
	@DisplayName("정식 로그인 성공은 사전 세션을 OTP 검증 단계에 결합한다")
	void loginBindsPreAuthenticationSession() {
		AdminAccount admin = readyAdmin();
		given(queryAdminAccountPort.findAccountByLoginId("ops-june")).willReturn(Optional.of(admin));
		given(adminPasswordPort.matches("AdminPass1!", admin.getPasswordHash())).willReturn(true);

		AdminLoginChallengeResult result = service().login("raw-session", "ops-june", "AdminPass1!");

		assertThat(result.nextStep()).isEqualTo(AdminAuthStep.VERIFY_OTP.name());
		then(adminSessionLifecyclePort).should().bindPreAuthentication(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(admin.getId()),
				org.mockito.ArgumentMatchers.eq(admin.getAuthenticationVersion()),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.LOGIN_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
		then(telemetryPort).should().loginSucceeded("official", admin.getId());
	}

	@Test
	@DisplayName("정식 로그인 실패는 계정 존재 여부 없이 제한된 실패 사유만 기록한다")
	void loginFailureRecordsGenericReason() {
		given(queryAdminAccountPort.findAccountByLoginId("missing-admin")).willReturn(Optional.empty());

		assertThatThrownBy(() -> service().login("raw-session", "missing-admin", "WrongPass1!"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.INVALID_CREDENTIALS));

		then(telemetryPort).should().loginRejected("official", "invalid_credentials");
	}

	@Test
	@DisplayName("정식 로그인 계정 저장소 장애는 인증 저장소 예외로 변환한다")
	void loginStoreFailureIsServiceUnavailable() {
		given(queryAdminAccountPort.findAccountByLoginId("ops-june"))
				.willThrow(new DataAccessResourceFailureException("db unavailable"));

		assertThatThrownBy(() -> service().login("raw-session", "ops-june", "AdminPass1!"))
				.isInstanceOf(AdminAuthenticationStoreException.class);
	}

	@Test
	@DisplayName("잘못된 로그인 아이디 형식은 저장소 장애가 아니라 입력 오류로 유지한다")
	void invalidLoginIdRemainsDomainValidationFailure() {
		assertThatThrownBy(() -> service().login("raw-session", "", "AdminPass1!"))
				.isInstanceOf(AdminDomainException.class);
	}

	@Test
	@DisplayName("OTP 성공은 사전 세션 자격 증명을 인증 완료 세션으로 교체한다")
	void verifyOtpCompletesSessionAuthentication() {
		AdminAccount admin = readyAdmin();
		given(adminSessionLifecyclePort.requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.LOGIN_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class))).willReturn(admin);
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("plain-secret");
		given(adminOtpPort.verify("plain-secret", "123456")).willReturn(true);
		given(adminSessionLifecyclePort.completeAuthentication(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.same(admin),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.LOGIN_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
				.willReturn(new AdminSessionCredentialResult("new-session", "new-csrf"));

		AdminAuthenticationResult result = service().verifyOtp("raw-session", "123456");

		assertThat(result.credentials()).isEqualTo(new AdminSessionCredentialResult("new-session", "new-csrf"));
		assertThat(result.nickname()).isEqualTo("운영자1");
		assertThat(result.role()).isEqualTo(AdminRole.OPERATOR);
		assertThat(admin.getAuthenticationVersion()).isEqualTo(3L);
		then(adminSessionLifecyclePort).should().requirePhaseForUpdate(
				org.mockito.ArgumentMatchers.eq("raw-session"),
				org.mockito.ArgumentMatchers.eq(AdminSessionPhase.LOGIN_VERIFY_OTP),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
		then(telemetryPort).should().otpSucceeded("official", admin.getId());
	}

	@Test
	@DisplayName("로그아웃은 인증 버전을 증가시키고 현재 세션을 폐기한다")
	void logoutInvalidatesCurrentAuthentication() {
		AdminAccount admin = readyAdmin();
		given(queryAdminAccountPort.findAccount(admin.getId())).willReturn(Optional.of(admin));
		long before = admin.getAuthenticationVersion();

		service().logout(admin.getId(), "session-1");

		assertThat(admin.getAuthenticationVersion()).isEqualTo(before + 1);
		then(adminSessionLifecyclePort).should().revokeCurrent(
				org.mockito.ArgumentMatchers.eq(admin.getId()),
				org.mockito.ArgumentMatchers.eq("session-1"),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));
	}

	private AdminAuthService service() {
		return new AdminAuthService(queryAdminAccountPort, adminPasswordPort, adminOtpPort,
				protectAdminOtpSecretPort, adminSessionLifecyclePort, telemetryPort);
	}

	private AdminAccount readyAdmin() {
		AdminAccount admin = AdminAccount.invite(
				"operator@pikume.com", "운영자1", AdminRole.OPERATOR, "temp-hash",
				LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
		admin.completeCredentialSetup("ops-june", "password-hash");
		admin.startOtpRegistration("protected-secret");
		admin.completeOtpRegistration();
		return admin;
	}
}
