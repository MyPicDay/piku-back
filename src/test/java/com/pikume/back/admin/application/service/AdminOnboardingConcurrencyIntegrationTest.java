package com.pikume.back.admin.application.service;

import com.pikume.back.admin.adapter.out.persistence.AdminAccountJpaRepository;
import com.pikume.back.admin.adapter.out.persistence.AdminSessionJpaRepository;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionCachePort;
import com.pikume.back.admin.application.port.out.AdminSessionCredentialPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;
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

@SpringBootTest
@DisplayName("관리자 온보딩 동시성")
class AdminOnboardingConcurrencyIntegrationTest {

	@Autowired AdminOnboardingUseCase adminOnboardingUseCase;
	@Autowired AdminAccountJpaRepository adminAccountJpaRepository;
	@Autowired AdminSessionJpaRepository adminSessionJpaRepository;
	@Autowired AdminSessionCredentialPort adminSessionCredentialPort;

	@MockitoBean AdminSessionCachePort adminSessionCachePort;

	@AfterEach
	void tearDown() {
		adminSessionJpaRepository.deleteAll();
		adminAccountJpaRepository.deleteAll();
	}

	@Test
	@DisplayName("두 계정의 동일 로그인 아이디 동시 설정은 한 건만 성공하고 다른 한 건은 409로 거부한다")
	void concurrentSameLoginIdKeepsOneCredentialOwner() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount firstAdmin = invitedAdmin("first-operator@pikume.com", "첫운영자", now);
		AdminAccount secondAdmin = invitedAdmin("second-operator@pikume.com", "둘운영자", now);
		adminAccountJpaRepository.saveAndFlush(firstAdmin);
		adminAccountJpaRepository.saveAndFlush(secondAdmin);
		storePreAuthenticationSession("raw-session-1", firstAdmin, now);
		storePreAuthenticationSession("raw-session-2", secondAdmin, now);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Object> first = executor.submit(
					() -> setCredentialsAfterBarrier("raw-session-1", ready, start));
			Future<Object> second = executor.submit(
					() -> setCredentialsAfterBarrier("raw-session-2", ready, start));
			ready.await();
			start.countDown();

			List<Object> outcomes = List.of(first.get(), second.get());

			assertThat(outcomes).containsExactlyInAnyOrder("success", AdminErrorCode.DUPLICATE_LOGIN_ID);
			assertThat(adminAccountJpaRepository.findByLoginId("shared-ops")).isPresent();
			assertThat(adminAccountJpaRepository.findAll())
					.filteredOn(admin -> "shared-ops".equals(admin.getLoginId()))
					.hasSize(1);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	@DisplayName("같은 계정의 두 사전 세션은 자격 증명을 동시에 설정할 수 없다")
	void concurrentCredentialsForSameAccountAllowOnlyOneSession() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		AdminAccount admin = invitedAdmin("operator@pikume.com", "운영자", now);
		adminAccountJpaRepository.saveAndFlush(admin);
		storePreAuthenticationSession("raw-session-1", admin, now);
		storePreAuthenticationSession("raw-session-2", admin, now);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Object> first = executor.submit(
					() -> setCredentialsAfterBarrier("raw-session-1", "first-ops", ready, start));
			Future<Object> second = executor.submit(
					() -> setCredentialsAfterBarrier("raw-session-2", "second-ops", ready, start));
			ready.await();
			start.countDown();

			List<Object> outcomes = List.of(first.get(), second.get());
			AdminAccount persisted = adminAccountJpaRepository.findById(admin.getId()).orElseThrow();

			assertThat(outcomes).containsExactlyInAnyOrder("success", AdminErrorCode.UNAUTHENTICATED);
			assertThat(persisted.getLoginId()).isIn("first-ops", "second-ops");
			assertThat(persisted.getAuthenticationVersion()).isEqualTo(admin.getAuthenticationVersion() + 1);
		} finally {
			executor.shutdownNow();
		}
	}

	private Object setCredentialsAfterBarrier(
			String sessionToken, CountDownLatch ready, CountDownLatch start) {
		return setCredentialsAfterBarrier(sessionToken, "shared-ops", ready, start);
	}

	private Object setCredentialsAfterBarrier(
			String sessionToken, String loginId, CountDownLatch ready, CountDownLatch start) {
		try {
			ready.countDown();
			start.await();
			adminOnboardingUseCase.setCredentials(sessionToken, loginId, "Password1!");
			return "success";
		} catch (AdminException exception) {
			return exception.errorCode();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private AdminAccount invitedAdmin(String email, String nickname, LocalDateTime now) {
		return AdminAccount.invite(
				email,
				nickname,
				AdminRole.OPERATOR,
				"temporary-password-hash",
				now.minusMinutes(1),
				now.plusHours(24));
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
				AdminSessionPhase.ONBOARDING_SET_CREDENTIALS,
				now.plusMinutes(10),
				now);
		adminSessionJpaRepository.saveAndFlush(session);
	}
}
