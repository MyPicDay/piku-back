package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.AdminSessionLifecyclePort;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminLoginId;
import com.pikume.back.admin.domain.AdminSessionPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AdminAuthService implements AdminAuthUseCase {

	private static final String AUTHENTICATION_FLOW = "official";

	private final LoadAdminAccountPort loadAdminAccountPort;
	private final AdminPasswordPort adminPasswordPort;
	private final AdminOtpPort adminOtpPort;
	private final ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	private final AdminSessionLifecyclePort adminSessionLifecyclePort;
	private final AdminSessionTelemetryPort telemetryPort;

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminLoginChallengeResult login(String sessionToken, String loginId, String password) {
		String normalizedLoginId = AdminLoginId.normalize(loginId);
		AdminAccount admin = loadAccount(() -> loadAdminAccountPort.findByLoginId(normalizedLoginId))
				.orElseThrow(() -> rejectLogin(
						AdminProblem.INVALID_CREDENTIALS,
						"관리자 로그인 정보가 올바르지 않습니다.",
						"invalid_credentials"));
		LocalDateTime now = LocalDateTime.now();
		admin.releaseExpiredLock(now);
		if (admin.isLockedAt(now)) {
			throw rejectLogin(AdminProblem.ACCOUNT_LOCKED, "관리자 계정이 잠겨 있습니다.", "account_locked");
		}
		if (!canUseOfficialLogin(admin)) {
			throw rejectLogin(
					AdminProblem.INVALID_CREDENTIALS,
					"관리자 로그인 정보가 올바르지 않습니다.",
					"invalid_credentials");
		}
		if (!StringUtils.hasText(password) || !adminPasswordPort.matches(password, admin.getPasswordHash())) {
			admin.recordPasswordFailure(now);
			if (admin.isLockedAt(now)) {
				throw rejectLogin(AdminProblem.ACCOUNT_LOCKED, "관리자 계정이 잠겨 있습니다.", "account_locked");
			}
			throw rejectLogin(
					AdminProblem.INVALID_CREDENTIALS,
					"관리자 로그인 정보가 올바르지 않습니다.",
					"invalid_credentials");
		}
		adminSessionLifecyclePort.bindPreAuthentication(sessionToken, admin.getId(), admin.getAuthenticationVersion(),
				AdminSessionPhase.LOGIN_VERIFY_OTP, now);
		telemetryPort.loginSucceeded(AUTHENTICATION_FLOW, admin.getId());
		return new AdminLoginChallengeResult(AdminAuthStep.VERIFY_OTP.name(),
				admin.getLoginId(), admin.getNickname(), admin.getEmail(), admin.getRole());
	}

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminAuthenticationResult verifyOtp(String sessionToken, String otpCode) {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount admin = adminSessionLifecyclePort.requirePhaseForUpdate(
				sessionToken, AdminSessionPhase.LOGIN_VERIFY_OTP, now);
		if (!canUseOfficialLogin(admin)) {
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "invalid_credentials");
			throw invalidCredentials();
		}
		if (admin.isOtpBlockedAt(now)) {
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "blocked");
			throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
		}
		if (!StringUtils.hasText(admin.getOtpSecret())) {
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "not_registered");
			throw new AdminException(AdminProblem.INVALID_REQUEST, "OTP가 등록되어 있지 않습니다.");
		}
		String secret = protectAdminOtpSecretPort.reveal(admin.getOtpSecret());
		if (!adminOtpPort.verify(secret, otpCode)) {
			admin.recordOtpFailure(now);
			if (admin.isOtpBlockedAt(now)) {
				telemetryPort.otpRejected(AUTHENTICATION_FLOW, "blocked");
				throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
			}
			telemetryPort.otpRejected(AUTHENTICATION_FLOW, "invalid_code");
			throw new AdminException(AdminProblem.OTP_VERIFICATION_FAILED, "OTP 인증 코드가 올바르지 않습니다.");
		}
		admin.resetOtpFailures();
		admin.recordLoginSuccess(now);
		admin.advanceAuthenticationVersion();
		AdminSessionCredentials credentials = adminSessionLifecyclePort.completeAuthentication(
				sessionToken, admin, AdminSessionPhase.LOGIN_VERIFY_OTP, now);
		telemetryPort.otpSucceeded(AUTHENTICATION_FLOW, admin.getId());
		return result(admin, credentials);
	}

	@Override
	@Transactional
	public void logout(String adminId, String sessionId) {
		AdminAccount admin = requireAdmin(adminId);
		admin.advanceAuthenticationVersion();
		adminSessionLifecyclePort.revokeCurrent(adminId, sessionId, LocalDateTime.now());
	}

	@Override
	@Transactional
	public void changePassword(String adminId, String currentPassword, String newPassword) {
		AdminAccount admin = requireAdmin(adminId);
		if (!StringUtils.hasText(admin.getPasswordHash()) || !StringUtils.hasText(currentPassword)
				|| !adminPasswordPort.matches(currentPassword, admin.getPasswordHash())) {
			throw invalidCredentials();
		}
		validatePassword(newPassword, admin.getLoginId());
		admin.changePassword(adminPasswordPort.encode(newPassword));
		adminSessionLifecyclePort.revokeActiveSessions(admin.getId(), LocalDateTime.now());
	}

	private AdminAuthenticationResult result(AdminAccount admin, AdminSessionCredentials credentials) {
		return new AdminAuthenticationResult(
				credentials, admin.getLoginId(), admin.getNickname(), admin.getEmail(), admin.getRole());
	}

	private boolean canUseOfficialLogin(AdminAccount admin) {
		return admin.getStatus() == AdminAccountStatus.ACTIVE && admin.hasLoginId()
				&& !admin.isPasswordChangeRequired() && admin.isOtpRegistered();
	}

	private AdminAccount requireAdmin(String adminId) {
		return loadAccount(() -> loadAdminAccountPort.findById(adminId))
				.orElseThrow(this::invalidCredentials);
	}

	private AdminException invalidCredentials() {
		return new AdminException(AdminProblem.INVALID_CREDENTIALS, "관리자 로그인 정보가 올바르지 않습니다.");
	}

	private AdminException rejectLogin(AdminProblem problem, String detail, String reason) {
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
		if (password == null || password.length() < 8 || password.length() > 20
				|| password.chars().anyMatch(Character::isWhitespace)
				|| password.chars().anyMatch(ch -> ch > 127)
				|| password.equals(loginId) || password.contains(loginId)
				|| !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
				|| !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "관리자 패스워드 정책을 만족해야 합니다.");
		}
	}
}
