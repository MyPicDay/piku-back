package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.out.HashAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.application.port.out.SaveAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.SaveAdminSessionPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminId;
import com.pikume.back.admin.domain.AdminRefreshToken;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.security.jwt.AdminAuthConstants;
import com.pikume.back.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminSessionTokenService {

	private final LoadAdminSessionPort loadAdminSessionPort;
	private final SaveAdminSessionPort saveAdminSessionPort;
	private final LoadAdminRefreshTokenPort loadAdminRefreshTokenPort;
	private final SaveAdminRefreshTokenPort saveAdminRefreshTokenPort;
	private final HashAdminRefreshTokenPort hashAdminRefreshTokenPort;
	private final JwtProvider jwtProvider;

	public AdminTokenIssueResult issueNewSession(AdminAccount admin, LocalDateTime now) {
		revokeActiveSessions(admin.getId(), now);

		String sessionId = AdminId.newId();
		LocalDateTime absoluteExpiresAt = now.plus(Duration.ofMillis(AdminAuthConstants.REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME));
		LocalDateTime refreshExpiresAt = refreshExpiresAt(now, absoluteExpiresAt);
		String refreshToken = jwtProvider.generateAdminRefreshToken(admin.getId(), sessionId);
		String refreshTokenHash = hashAdminRefreshTokenPort.hash(refreshToken);

		AdminSession session = AdminSession.start(
				sessionId,
				admin.getId(),
				refreshTokenHash,
				absoluteExpiresAt,
				refreshExpiresAt,
				now);
		saveAdminSessionPort.save(session);
		saveAdminRefreshTokenPort.save(AdminRefreshToken.issue(
				refreshTokenHash,
				sessionId,
				admin.getId(),
				now,
				refreshExpiresAt));

		return tokenResult(admin, sessionId, refreshToken, refreshExpiresAt, now);
	}

	public AdminTokenIssueResult rotate(AdminAccount admin, AdminSession session,
			AdminRefreshToken previousRefreshToken, LocalDateTime now) {
		LocalDateTime refreshExpiresAt = refreshExpiresAt(now, session.getAbsoluteExpiresAt());
		String refreshToken = jwtProvider.generateAdminRefreshToken(admin.getId(), session.getId());
		String refreshTokenHash = hashAdminRefreshTokenPort.hash(refreshToken);

		previousRefreshToken.rotate(now);
		saveAdminRefreshTokenPort.save(previousRefreshToken);
		session.rotate(refreshTokenHash, refreshExpiresAt, now);
		saveAdminSessionPort.save(session);
		saveAdminRefreshTokenPort.save(AdminRefreshToken.issue(
				refreshTokenHash,
				session.getId(),
				admin.getId(),
				now,
				refreshExpiresAt));

		return tokenResult(admin, session.getId(), refreshToken, refreshExpiresAt, now);
	}

	public void revokeSession(AdminSession session, LocalDateTime now) {
		loadAdminRefreshTokenPort.findActiveBySessionId(session.getId())
				.forEach(refreshToken -> {
					refreshToken.revoke(now);
					saveAdminRefreshTokenPort.save(refreshToken);
				});
		session.revoke(now);
		saveAdminSessionPort.save(session);
	}

	public void revokeActiveSessions(String adminId, LocalDateTime now) {
		revokeActiveSessionsForAdmin(adminId, now);
	}

	public void markRefreshTokenReuse(AdminSession session, AdminRefreshToken reusedRefreshToken, LocalDateTime now) {
		reusedRefreshToken.markReused(now);
		saveAdminRefreshTokenPort.save(reusedRefreshToken);
		loadAdminRefreshTokenPort.findActiveBySessionId(session.getId())
				.forEach(refreshToken -> {
					refreshToken.revoke(now);
					saveAdminRefreshTokenPort.save(refreshToken);
				});
		session.markReuseDetected(now);
		saveAdminSessionPort.save(session);
	}

	private void revokeActiveSessionsForAdmin(String adminId, LocalDateTime now) {
		loadAdminSessionPort.findActiveByAdminId(adminId)
				.forEach(session -> revokeSession(session, now));
	}

	private LocalDateTime refreshExpiresAt(LocalDateTime now, LocalDateTime absoluteExpiresAt) {
		LocalDateTime idleExpiresAt = now.plus(Duration.ofMillis(AdminAuthConstants.REFRESH_TOKEN_IDLE_EXPIRATION_TIME));
		if (idleExpiresAt.isAfter(absoluteExpiresAt)) {
			return absoluteExpiresAt;
		}
		return idleExpiresAt;
	}

	private AdminTokenIssueResult tokenResult(AdminAccount admin, String sessionId, String refreshToken,
			LocalDateTime refreshExpiresAt, LocalDateTime now) {
		String accessToken = jwtProvider.generateAdminAccessToken(admin.getId(), admin.getRole().name(), sessionId);
		return new AdminTokenIssueResult(
				accessToken,
				refreshToken,
				AdminAuthConstants.ACCESS_TOKEN_EXPIRATION_TIME / 1000L,
				Duration.between(now, refreshExpiresAt).toSeconds(),
				sessionId,
				admin.getLoginId(),
				admin.getNickname(),
				admin.getEmail(),
				admin.getRole());
	}
}
