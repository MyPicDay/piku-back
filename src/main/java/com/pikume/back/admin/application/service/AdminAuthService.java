package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
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
import com.pikume.back.admin.domain.AdminLoginId;
import com.pikume.back.admin.domain.AdminRefreshToken;
import com.pikume.back.admin.domain.AdminSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService implements AdminAuthUseCase {

	private final LoadAdminAccountPort loadAdminAccountPort;
	private final LoadAdminSessionPort loadAdminSessionPort;
	private final LoadAdminRefreshTokenPort loadAdminRefreshTokenPort;
	private final HashAdminRefreshTokenPort hashAdminRefreshTokenPort;
	private final PasswordEncoder passwordEncoder;
	private final AdminTokenPort adminTokenPort;
	private final AdminOtpPort adminOtpPort;
	private final ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	private final AdminSessionTokenService adminSessionTokenService;

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminLoginChallengeResult login(String loginId, String password) {
		String normalizedLoginId = AdminLoginId.normalize(loginId);
		AdminAccount admin = loadAdminAccountPort.findByLoginId(normalizedLoginId)
				.orElseThrow(this::invalidCredentials);
		LocalDateTime now = LocalDateTime.now();
		admin.releaseExpiredLock(now);

		if (admin.isLockedAt(now)) {
			throw new AdminException(AdminProblem.ACCOUNT_LOCKED, "관리자 계정이 잠겨 있습니다.");
		}
		if (!canUseOfficialLogin(admin)) {
			throw invalidCredentials();
		}
		if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, admin.getPasswordHash())) {
			admin.recordPasswordFailure(now);
			if (admin.isLockedAt(now)) {
				throw new AdminException(AdminProblem.ACCOUNT_LOCKED, "관리자 계정이 잠겨 있습니다.");
			}
			throw invalidCredentials();
		}

		String otpChallengeToken = adminTokenPort.generateOtpChallengeToken(admin.getId());
		return new AdminLoginChallengeResult(
				otpChallengeToken,
				adminTokenPort.otpChallengeTokenTtl().toSeconds(),
				AdminAuthStep.VERIFY_OTP.name(),
				admin.getLoginId(),
				admin.getNickname(),
				admin.getEmail(),
				admin.getRole());
	}

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminTokenIssueResult verifyOtp(String adminId, String otpCode) {
		AdminAccount admin = requireAdmin(adminId);
		LocalDateTime now = LocalDateTime.now();
		if (!canUseOfficialLogin(admin)) {
			throw invalidCredentials();
		}
		if (admin.isOtpBlockedAt(now)) {
			throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
		}

		String otpSecret = admin.getOtpSecret();
		if (!StringUtils.hasText(otpSecret)) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "OTP가 등록되어 있지 않습니다.");
		}
			String plainSecret = protectAdminOtpSecretPort.reveal(otpSecret);
			if (!adminOtpPort.verify(plainSecret, otpCode)) {
				admin.recordOtpFailure(now);
				if (admin.isOtpBlockedAt(now)) {
					throw new AdminException(AdminProblem.OTP_BLOCKED, "OTP 인증이 일시적으로 차단되었습니다.");
				}
				throw new AdminException(AdminProblem.OTP_VERIFICATION_FAILED, "OTP 인증 코드가 올바르지 않습니다.");
			}

		admin.resetOtpFailures();
		admin.recordLoginSuccess(now);
		return adminSessionTokenService.issueNewSession(admin, now);
	}

	@Override
	@Transactional(noRollbackFor = AdminException.class)
	public AdminTokenIssueResult reissue(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			throw invalidRefreshToken();
		}

		LocalDateTime now = LocalDateTime.now();
		String tokenHash = hashAdminRefreshTokenPort.hash(refreshToken);
		AdminRefreshToken storedRefreshToken = loadAdminRefreshTokenPort.findByTokenHash(tokenHash)
				.orElseThrow(this::invalidRefreshToken);

		AdminSession session = loadAdminSessionPort.findById(storedRefreshToken.getSessionId())
				.orElseThrow(this::invalidRefreshToken);
		if (storedRefreshToken.isRotated()) {
			adminSessionTokenService.markRefreshTokenReuse(session, storedRefreshToken, now);
			throw new AdminException(AdminProblem.REFRESH_TOKEN_REUSED, "이미 회전된 관리자 Refresh Token이 재사용되었습니다.");
		}
		AdminRefreshTokenClaims tokenClaims = adminTokenPort.readValidRefreshToken(refreshToken)
				.orElse(null);
		if (tokenClaims == null) {
			storedRefreshToken.revoke(now);
			throw invalidRefreshToken();
		}

		String adminId = tokenClaims.adminId();
		String sessionId = tokenClaims.sessionId();
		if (!storedRefreshToken.belongsTo(adminId)
				|| !storedRefreshToken.matchesSession(sessionId)
				|| !session.belongsTo(adminId)) {
			adminSessionTokenService.markRefreshTokenReuse(session, storedRefreshToken, now);
			throw new AdminException(AdminProblem.REFRESH_TOKEN_REUSED, "관리자 Refresh Token 세션 정보가 일치하지 않습니다.");
		}
		if (!storedRefreshToken.isActiveAt(now) || !session.isActiveAt(now)) {
			storedRefreshToken.expire(now);
			session.expire(now);
			throw invalidRefreshToken();
		}

		AdminAccount admin = requireAdmin(adminId);
		if (admin.getStatus() != AdminAccountStatus.ACTIVE) {
			adminSessionTokenService.revokeSession(session, now);
			throw invalidRefreshToken();
		}
		return adminSessionTokenService.rotate(admin, session, storedRefreshToken, now);
	}

	@Override
	@Transactional
	public void logout(String adminId, String sessionId) {
		if (!StringUtils.hasText(adminId) || !StringUtils.hasText(sessionId)) {
			return;
		}
		loadAdminSessionPort.findById(sessionId)
				.filter(session -> session.belongsTo(adminId))
				.ifPresent(session -> adminSessionTokenService.revokeSession(session, LocalDateTime.now()));
	}

	@Override
	@Transactional
	public void changePassword(String adminId, String currentPassword, String newPassword) {
		AdminAccount admin = requireAdmin(adminId);
		if (!StringUtils.hasText(admin.getPasswordHash())
				|| !StringUtils.hasText(currentPassword)
				|| !passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
			throw invalidCredentials();
		}
		validatePassword(newPassword, admin.getLoginId());
		admin.completePasswordSetup(passwordEncoder.encode(newPassword));
		adminSessionTokenService.revokeActiveSessions(admin.getId(), LocalDateTime.now());
	}

	private boolean canUseOfficialLogin(AdminAccount admin) {
		return admin.getStatus() == AdminAccountStatus.ACTIVE
				&& admin.hasLoginId()
				&& !admin.isPasswordChangeRequired()
				&& admin.isOtpRegistered();
	}

	private AdminAccount requireAdmin(String adminId) {
		return loadAdminAccountPort.findById(adminId)
				.orElseThrow(this::invalidCredentials);
	}

	private AdminException invalidCredentials() {
		return new AdminException(AdminProblem.INVALID_CREDENTIALS, "관리자 로그인 정보가 올바르지 않습니다.");
	}

	private AdminException invalidRefreshToken() {
		return new AdminException(AdminProblem.INVALID_REFRESH_TOKEN, "유효하지 않은 관리자 Refresh Token입니다.");
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
