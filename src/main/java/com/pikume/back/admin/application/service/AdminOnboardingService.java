package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminEmail;
import com.pikume.back.admin.domain.AdminLoginId;
import com.pikume.back.security.jwt.AdminAuthConstants;
import com.pikume.back.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminOnboardingService implements AdminOnboardingUseCase {

	private static final String OTP_ISSUER = "Pikume Ops";

	private final LoadAdminAccountPort loadAdminAccountPort;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final AdminOtpPort adminOtpPort;
	private final ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	private final AdminSessionTokenService adminSessionTokenService;

	@Override
	@Transactional
	public AdminTemporaryLoginResult temporaryLogin(String email, String temporaryPassword) {
		String normalizedEmail = AdminEmail.normalize(email);
		AdminAccount admin = loadAdminAccountPort.findByEmail(normalizedEmail)
				.orElseThrow(() -> new AdminException(AdminProblem.INVALID_CREDENTIALS, "임시 로그인 정보가 올바르지 않습니다."));
		LocalDateTime now = LocalDateTime.now();
		admin.releaseExpiredLock(now);

		if (admin.hasLoginId()) {
			throw new AdminException(AdminProblem.INVALID_CREDENTIALS, "정식 로그인 아이디 설정 이후에는 이메일 로그인을 사용할 수 없습니다.");
		}
		if (!admin.canUseTemporaryCredentialAt(now)) {
			throw new AdminException(AdminProblem.TEMPORARY_CREDENTIAL_EXPIRED, "임시 로그인 정보가 만료되었습니다.");
		}
		if (temporaryPassword == null || temporaryPassword.isBlank()) {
			admin.recordPasswordFailure(now);
			throw new AdminException(AdminProblem.INVALID_CREDENTIALS, "임시 로그인 정보가 올바르지 않습니다.");
		}
		if (!passwordEncoder.matches(temporaryPassword, admin.getTemporaryPasswordHash())) {
			admin.recordPasswordFailure(now);
			throw new AdminException(AdminProblem.INVALID_CREDENTIALS, "임시 로그인 정보가 올바르지 않습니다.");
		}

		String token = jwtProvider.generateAdminOnboardingToken(admin.getId());
		return new AdminTemporaryLoginResult(
				token,
				AdminAuthConstants.ONBOARDING_TOKEN_EXPIRATION_TIME / 1000L,
				AdminOnboardingStep.SET_LOGIN_ID.name(),
				admin.getEmail(),
				admin.getNickname(),
				admin.getRole());
	}

	@Override
	@Transactional
	public void setLoginId(String adminId, String loginId) {
		String normalizedLoginId = AdminLoginId.normalize(loginId);
		if (loadAdminAccountPort.existsByLoginId(normalizedLoginId)) {
			throw new AdminException(AdminProblem.DUPLICATE_LOGIN_ID, "이미 사용 중인 관리자 로그인 아이디입니다.");
		}
		AdminAccount admin = requireAdmin(adminId);
		admin.setLoginId(normalizedLoginId);
	}

	@Override
	@Transactional
	public void setPassword(String adminId, String password) {
		AdminAccount admin = requireAdmin(adminId);
		if (!admin.hasLoginId()) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "정식 로그인 아이디를 먼저 설정해야 합니다.");
		}
		validatePassword(password, admin.getLoginId());
		admin.completePasswordSetup(passwordEncoder.encode(password));
	}

	@Override
	@Transactional
	public AdminOtpRegistrationResult startOtpRegistration(String adminId) {
		AdminAccount admin = requireAdmin(adminId);
		if (admin.isPasswordChangeRequired()) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "정식 패스워드를 먼저 설정해야 합니다.");
		}
		String secret = adminOtpPort.generateSecret();
		admin.startOtpRegistration(protectAdminOtpSecretPort.protect(secret));
		String accountName = admin.getLoginId();
		return new AdminOtpRegistrationResult(
				OTP_ISSUER,
				accountName,
				adminOtpPort.provisioningUri(OTP_ISSUER, accountName, secret),
				secret);
	}

	@Override
	@Transactional
	public AdminTokenIssueResult verifyOtp(String adminId, String otpCode) {
		AdminAccount admin = requireAdmin(adminId);
		if (admin.getPendingOtpSecret() == null) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "OTP 등록을 먼저 시작해야 합니다.");
		}
		LocalDateTime now = LocalDateTime.now();
		if (admin.isOtpBlockedAt(now)) {
			throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
		}
		String secret = protectAdminOtpSecretPort.reveal(admin.getPendingOtpSecret());
		if (!adminOtpPort.verify(secret, otpCode)) {
			admin.recordOtpFailure(now);
			throw new AdminException(AdminProblem.OTP_VERIFICATION_FAILED, "OTP 인증 코드가 올바르지 않습니다.");
		}
		admin.completeOtpRegistration();
		admin.recordLoginSuccess(now);
		return adminSessionTokenService.issueNewSession(admin, now);
	}

	private AdminAccount requireAdmin(String adminId) {
		return loadAdminAccountPort.findById(adminId)
				.orElseThrow(() -> new AdminException(AdminProblem.ONBOARDING_TOKEN_INVALID, "온보딩 토큰이 유효하지 않습니다."));
	}

	private void validatePassword(String password, String loginId) {
		if (password == null
				|| password.length() < 8
				|| password.length() > 20
				|| password.chars().anyMatch(Character::isWhitespace)
				|| password.chars().anyMatch(ch -> ch > 127)
				|| password.equals(loginId)
				|| password.contains(loginId)
				|| !password.matches(".*[A-Z].*")
				|| !password.matches(".*[a-z].*")
				|| !password.matches(".*\\d.*")
				|| !password.matches(".*[^A-Za-z0-9].*")) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "관리자 패스워드 정책을 만족해야 합니다.");
		}
	}
}
