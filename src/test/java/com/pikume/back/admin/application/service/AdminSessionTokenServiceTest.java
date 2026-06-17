package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.out.HashAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.LoadAdminSessionPort;
import com.pikume.back.admin.application.port.out.AdminTokenPort;
import com.pikume.back.admin.application.port.out.SaveAdminRefreshTokenPort;
import com.pikume.back.admin.application.port.out.SaveAdminSessionPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRefreshToken;
import com.pikume.back.admin.domain.AdminRefreshTokenStatus;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSessionTokenService")
class AdminSessionTokenServiceTest {

	@Mock
	private LoadAdminSessionPort loadAdminSessionPort;
	@Mock
	private SaveAdminSessionPort saveAdminSessionPort;
	@Mock
	private LoadAdminRefreshTokenPort loadAdminRefreshTokenPort;
	@Mock
	private SaveAdminRefreshTokenPort saveAdminRefreshTokenPort;
	@Mock
	private HashAdminRefreshTokenPort hashAdminRefreshTokenPort;
	@Mock
	private AdminTokenPort adminTokenPort;

	@Test
	@DisplayName("새 관리자 세션 발급 시 기존 활성 세션과 토큰을 폐기한다")
	void issueNewSessionRevokesExistingActiveSession() {
		AdminAccount admin = readyAdmin();
		AdminSession oldSession = activeSession(admin.getId(), "old-session", "old-hash");
		AdminRefreshToken oldRefreshToken = activeRefreshToken(admin.getId(), oldSession.getId(), "old-hash");
		given(loadAdminSessionPort.findActiveByAdminId(admin.getId())).willReturn(List.of(oldSession));
		given(loadAdminRefreshTokenPort.findActiveBySessionId(oldSession.getId())).willReturn(List.of(oldRefreshToken));
		given(adminTokenPort.refreshTokenAbsoluteTtl()).willReturn(Duration.ofHours(8));
		given(adminTokenPort.refreshTokenIdleTtl()).willReturn(Duration.ofMinutes(30));
		given(adminTokenPort.generateRefreshToken(eq(admin.getId()), anyString())).willReturn("new-refresh");
		given(hashAdminRefreshTokenPort.hash("new-refresh")).willReturn("new-hash");
		given(adminTokenPort.generateAccessToken(eq(admin.getId()), eq(AdminRole.OPERATOR.name()), anyString()))
				.willReturn("new-access");
		given(adminTokenPort.accessTokenTtl()).willReturn(Duration.ofMinutes(10));

		AdminTokenIssueResult result = service().issueNewSession(admin, LocalDateTime.now());

		assertThat(result.accessToken()).isEqualTo("new-access");
		assertThat(result.refreshToken()).isEqualTo("new-refresh");
		assertThat(oldSession.getStatus()).isEqualTo(AdminSessionStatus.REVOKED);
		assertThat(oldRefreshToken.getStatus()).isEqualTo(AdminRefreshTokenStatus.REVOKED);
	}

	@Test
	@DisplayName("관리자 Refresh Token 회전 시 이전 토큰을 ROTATED로 남기고 새 토큰을 저장한다")
	void rotateMarksPreviousRefreshTokenRotated() {
		AdminAccount admin = readyAdmin();
		AdminSession session = activeSession(admin.getId(), "session-1", "old-hash");
		AdminRefreshToken previousRefreshToken = activeRefreshToken(admin.getId(), session.getId(), "old-hash");
		given(adminTokenPort.refreshTokenIdleTtl()).willReturn(Duration.ofMinutes(30));
		given(adminTokenPort.generateRefreshToken(admin.getId(), session.getId())).willReturn("new-refresh");
		given(hashAdminRefreshTokenPort.hash("new-refresh")).willReturn("new-hash");
		given(adminTokenPort.generateAccessToken(admin.getId(), AdminRole.OPERATOR.name(), session.getId()))
				.willReturn("new-access");
		given(adminTokenPort.accessTokenTtl()).willReturn(Duration.ofMinutes(10));

		AdminTokenIssueResult result = service().rotate(admin, session, previousRefreshToken, LocalDateTime.now());

		ArgumentCaptor<AdminRefreshToken> tokenCaptor = ArgumentCaptor.forClass(AdminRefreshToken.class);
		assertThat(result.refreshToken()).isEqualTo("new-refresh");
		assertThat(previousRefreshToken.getStatus()).isEqualTo(AdminRefreshTokenStatus.ROTATED);
		assertThat(session.getCurrentRefreshTokenHash()).isEqualTo("new-hash");
		then(saveAdminRefreshTokenPort).should(times(2)).save(tokenCaptor.capture());
		assertThat(tokenCaptor.getAllValues()).anySatisfy(saved ->
				assertThat(saved.getTokenHash()).isEqualTo("new-hash"));
	}

	private AdminSessionTokenService service() {
		return new AdminSessionTokenService(
				loadAdminSessionPort,
				saveAdminSessionPort,
				loadAdminRefreshTokenPort,
				saveAdminRefreshTokenPort,
				hashAdminRefreshTokenPort,
				adminTokenPort);
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

	private AdminSession activeSession(String adminId, String sessionId, String tokenHash) {
		LocalDateTime now = LocalDateTime.now();
		return AdminSession.start(
				sessionId,
				adminId,
				tokenHash,
				now.plusHours(8),
				now.plusMinutes(30),
				now);
	}

	private AdminRefreshToken activeRefreshToken(String adminId, String sessionId, String tokenHash) {
		LocalDateTime now = LocalDateTime.now();
		return AdminRefreshToken.issue(tokenHash, sessionId, adminId, now.minusMinutes(1), now.plusMinutes(30));
	}
}
