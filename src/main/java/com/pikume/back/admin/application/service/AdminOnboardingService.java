package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.AdminSessionLifecyclePort;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.application.port.out.SaveAdminCredentialsPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminEmail;
import com.pikume.back.admin.domain.AdminLoginId;
import com.pikume.back.admin.domain.AdminSessionPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AdminOnboardingService implements AdminOnboardingUseCase {

	private static final String OTP_ISSUER = "Pikume Ops";
	private static final String AUTHENTICATION_FLOW = "onboarding";

	private final LoadAdminAccountPort loadAdminAccountPort;
	private final AdminPasswordPort adminPasswordPort;
	private final AdminOtpPort adminOtpPort;
	private final ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	private final AdminSessionLifecyclePort adminSessionLifecyclePort;
	private final AdminSessionTelemetryPort telemetryPort;
	private final SaveAdminCredentialsPort saveAdminCredentialsPort;

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminTemporaryLoginResult temporaryLogin(String sessionToken, String email, String temporaryPassword) {
		String normalizedEmail = AdminEmail.normalize(email);
		AdminAccount admin = loadAccount(() -> loadAdminAccountPort.findByEmail(normalizedEmail))
				.orElseThrow(() -> rejectTemporaryLogin(
						AdminProblem.INVALID_CREDENTIALS,
						"임시 로그인 정보가 올바르지 않습니다.",
						"invalid_credentials"));
		LocalDateTime now = LocalDateTime.now();
		admin.releaseExpiredLock(now);
		if (admin.isLockedAt(now)) {
			throw rejectTemporaryLogin(
					AdminProblem.ACCOUNT_LOCKED, "관리자 계정이 잠겨 있습니다.", "account_locked");
		}
		if (admin.hasLoginId()) {
			throw rejectTemporaryLogin(
					AdminProblem.INVALID_CREDENTIALS,
					"임시 로그인 정보가 올바르지 않습니다.",
					"invalid_credentials");
		}
		if (!admin.canUseTemporaryCredentialAt(now)) {
			throw rejectTemporaryLogin(
					AdminProblem.TEMPORARY_CREDENTIAL_EXPIRED,
					"임시 로그인 정보가 만료되었습니다.",
					"credential_expired");
		}
		if (temporaryPassword == null || temporaryPassword.isBlank()
				|| !adminPasswordPort.matches(temporaryPassword, admin.getTemporaryPasswordHash())) {
			throw passwordFailure(admin, now);
		}
		adminSessionLifecyclePort.bindPreAuthentication(sessionToken, admin.getId(), admin.getAuthenticationVersion(),
				AdminSessionPhase.ONBOARDING_SET_CREDENTIALS, now);
		telemetryPort.loginSucceeded(AUTHENTICATION_FLOW, admin.getId());
		return new AdminTemporaryLoginResult(
				AdminOnboardingStep.SET_CREDENTIALS.name(), admin.getNickname(), admin.getRole());
	}

	@Override
	@Transactional
	public void setCredentials(String sessionToken, String loginId, String password) {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount admin = adminSessionLifecyclePort.requirePhaseForUpdate(
				sessionToken, AdminSessionPhase.ONBOARDING_SET_CREDENTIALS, now);
		String normalizedLoginId = AdminLoginId.normalize(loginId);
		if (loadAccount(() -> loadAdminAccountPort.existsByLoginId(normalizedLoginId))) {
			throw new AdminException(AdminProblem.DUPLICATE_LOGIN_ID, "이미 사용 중인 관리자 로그인 아이디입니다.");
		}
		validatePassword(password, normalizedLoginId);
		admin.completeCredentialSetup(normalizedLoginId, adminPasswordPort.encode(password));
		if (!saveAdminCredentialsPort.saveIfLoginIdAvailable(admin)) {
			throw new AdminException(AdminProblem.DUPLICATE_LOGIN_ID, "이미 사용 중인 관리자 로그인 아이디입니다.");
		}
		adminSessionLifecyclePort.advancePhase(sessionToken, AdminSessionPhase.ONBOARDING_SET_CREDENTIALS,
				AdminSessionPhase.ONBOARDING_REGISTER_OTP, admin.getAuthenticationVersion(), now);
	}

	@Override
	@Transactional
	public AdminOtpRegistrationResult startOtpRegistration(String sessionToken) {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount admin = adminSessionLifecyclePort.requirePhaseForUpdate(
				sessionToken, AdminSessionPhase.ONBOARDING_REGISTER_OTP, now);
		String secret = adminOtpPort.generateSecret();
		admin.startOtpRegistration(protectAdminOtpSecretPort.protect(secret));
		adminSessionLifecyclePort.advancePhase(sessionToken, AdminSessionPhase.ONBOARDING_REGISTER_OTP,
				AdminSessionPhase.ONBOARDING_VERIFY_OTP, admin.getAuthenticationVersion(), now);
		return new AdminOtpRegistrationResult(
				OTP_ISSUER, admin.getId(),
				adminOtpPort.provisioningUri(OTP_ISSUER, admin.getId(), secret), secret);
	}

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminAuthenticationResult verifyOtp(String sessionToken, String otpCode) {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount admin = adminSessionLifecyclePort.requirePhaseForUpdate(
				sessionToken, AdminSessionPhase.ONBOARDING_VERIFY_OTP, now);
		if (admin.getPendingOtpSecret() == null) {
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "not_registered");
			throw new AdminException(AdminProblem.INVALID_REQUEST, "OTP 등록을 먼저 시작해야 합니다.");
		}
		if (admin.isOtpBlockedAt(now)) {
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "blocked");
			throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
		}
		String secret = protectAdminOtpSecretPort.reveal(admin.getPendingOtpSecret());
		if (!adminOtpPort.verify(secret, otpCode)) {
			admin.recordOtpFailure(now);
			if (admin.isOtpBlockedAt(now)) {
				telemetryPort.otpRejected(AUTHENTICATION_FLOW, "blocked");
				throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
			}
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "invalid_code");
			throw new AdminException(AdminProblem.OTP_VERIFICATION_FAILED, "OTP 인증 코드가 올바르지 않습니다.");
		}
		admin.completeOtpRegistration();
		admin.recordLoginSuccess(now);
		admin.advanceAuthenticationVersion();
		AdminSessionCredentials credentials = adminSessionLifecyclePort.completeAuthentication(
				sessionToken, admin, AdminSessionPhase.ONBOARDING_VERIFY_OTP, now);
		telemetryPort.otpSucceeded(AUTHENTICATION_FLOW, admin.getId());
		return new AdminAuthenticationResult(
				credentials, admin.getNickname(), admin.getRole());
	}

	private AdminException passwordFailure(AdminAccount admin, LocalDateTime now) {
		admin.recordPasswordFailure(now);
		return admin.isLockedAt(now)
				? rejectTemporaryLogin(
						AdminProblem.ACCOUNT_LOCKED, "관리자 계정이 잠겨 있습니다.", "account_locked")
				: rejectTemporaryLogin(
						AdminProblem.INVALID_CREDENTIALS,
						"임시 로그인 정보가 올바르지 않습니다.",
						"invalid_credentials");
	}

	private AdminException rejectTemporaryLogin(AdminProblem problem, String detail, String reason) {
		telemetryPort.loginRejected(AUTHENTICATION_FLOW, reason);
		return new AdminException(problem, detail);
	}

	private <T> T loadAccount(Supplier<T> load) {
		try {
			return load.get();
		} catch (AdminAuthenticationStoreException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new AdminAuthenticationStoreException(
					"관리자 인증 저장소를 사용할 수 없습니다.", exception);
		}
	}

	private void validatePassword(String password, String loginId) {
		if (password == null || loginId == null || password.length() < 8 || password.length() > 20
				|| password.chars().anyMatch(Character::isWhitespace)
				|| password.chars().anyMatch(ch -> ch > 127)
				|| password.equals(loginId) || password.contains(loginId)
				|| !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
				|| !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "관리자 패스워드 정책을 만족해야 합니다.");
		}
	}
}
