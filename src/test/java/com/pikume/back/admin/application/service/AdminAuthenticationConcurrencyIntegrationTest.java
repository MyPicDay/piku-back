package com.pikume.back.admin.application.service;

import com.pikume.back.admin.adapter.out.persistence.AdminAccountJpaRepository;
import com.pikume.back.admin.adapter.out.persistence.AdminSessionJpaRepository;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
import com.pikume.back.admin.application.port.out.AdminOtpPort;
import com.pikume.back.admin.application.port.out.AdminSessionCachePort;
import com.pikume.back.admin.application.port.out.AdminSessionCredentialPort;
import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;
import com.pikume.back.admin.domain.AdminSessionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@DisplayName("관리자 OTP 인증 동시성")
class AdminAuthenticationConcurrencyIntegrationTest {

	@Autowired
	private AdminAuthUseCase adminAuthUseCase;
	@Autowired
	private AdminAccountJpaRepository adminAccountJpaRepository;
	@Autowired
	private AdminSessionJpaRepository adminSessionJpaRepository;
	@Autowired
	private AdminSessionCredentialPort adminSessionCredentialPort;

	@MockitoBean
	private AdminOtpPort adminOtpPort;
	@MockitoBean
	private ProtectAdminOtpSecretPort protectAdminOtpSecretPort;
	@MockitoBean
	private AdminSessionCachePort adminSessionCachePort;

	@AfterEach
	void tearDown() {
		adminSessionJpaRepository.deleteAll();
		adminAccountJpaRepository.deleteAll();
	}

	@Test
	@DisplayName("동시 OTP 성공 요청에서도 하나의 인증 완료 세션만 남는다")
	void concurrentOtpSuccessKeepsSingleAuthenticatedSession() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount admin = readyAdmin(now);
		adminAccountJpaRepository.saveAndFlush(admin);
		storePreAuthenticationSession("raw-session-1", admin, now);
		storePreAuthenticationSession("raw-session-2", admin, now);
		given(protectAdminOtpSecretPort.reveal("protected-secret")).willReturn("plain-secret");
		given(adminOtpPort.verify("plain-secret", "123456")).willReturn(true);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Object> first = executor.submit(() -> verifyAfterBarrier("raw-session-1", ready, start));
			Future<Object> second = executor.submit(() -> verifyAfterBarrier("raw-session-2", ready, start));
			ready.await();
			start.countDown();

			List<Object> outcomes = List.of(first.get(), second.get());

			List<AdminSession> activeSessions =
					adminSessionJpaRepository.findByAdminIdAndStatus(admin.getId(), AdminSessionStatus.ACTIVE)
							.stream()
							.filter(AdminSession::isAuthenticated)
							.toList();
			assertThat(outcomes).anyMatch(AdminAuthenticationResult.class::isInstance);
			assertThat(outcomes).allMatch(outcome ->
					outcome instanceof AdminAuthenticationResult || outcome instanceof AdminException);
			assertThat(activeSessions).hasSize(1);
			String activeTokenHash = activeSessions.get(0).getSessionTokenHash();
			assertThat(outcomes)
					.filteredOn(AdminAuthenticationResult.class::isInstance)
					.map(AdminAuthenticationResult.class::cast)
					.map(result -> adminSessionCredentialPort.hash(result.credentials().sessionToken()))
					.filteredOn(activeTokenHash::equals)
					.hasSize(1);
		} finally {
			executor.shutdownNow();
		}
	}

	private Object verifyAfterBarrier(String sessionToken, CountDownLatch ready, CountDownLatch start) {
		try {
			ready.countDown();
			start.await();
			return adminAuthUseCase.verifyOtp(sessionToken, "123456");
		} catch (AdminException exception) {
			return exception;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private AdminAccount readyAdmin(LocalDateTime now) {
		AdminAccount admin = AdminAccount.invite(
				"concurrent-admin@pikume.com",
				"동시성관리자",
				AdminRole.OPERATOR,
				"temporary-password-hash",
				now.minusDays(1),
				now.plusDays(1));
		admin.completeCredentialSetup("concurrent1", "password-hash");
		admin.startOtpRegistration("protected-secret");
		admin.completeOtpRegistration();
		return admin;
	}

	private void storePreAuthenticationSession(String rawSessionToken, AdminAccount admin, LocalDateTime now) {
		AdminSession session = AdminSession.startAnonymous(
				adminSessionCredentialPort.hash(rawSessionToken),
				adminSessionCredentialPort.hash("csrf-" + rawSessionToken),
				now,
				now.plusMinutes(10));
		session.bindAdmin(
				admin.getId(),
				admin.getAuthenticationVersion(),
				AdminSessionPhase.LOGIN_VERIFY_OTP,
				now.plusMinutes(10),
				now);
		adminSessionJpaRepository.saveAndFlush(session);
	}
}
