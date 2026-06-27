package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Admin transactional boundary")
class AdminTransactionalBoundaryTest {

	@Test
	@DisplayName("인증 실패 상태를 저장해야 하는 공식 로그인 메서드는 AdminException으로 롤백하지 않는다")
	void authFailureStateMethodsDoNotRollbackForAdminException() throws Exception {
		assertThat(noRollbackFor(AdminAuthService.class, "login", String.class, String.class, String.class))
				.contains(AdminException.class);
		assertThat(noRollbackFor(AdminAuthService.class, "verifyOtp", String.class, String.class))
				.contains(AdminException.class);
	}

	@Test
	@DisplayName("인증 실패 상태를 저장해야 하는 온보딩 메서드는 AdminException으로 롤백하지 않는다")
	void onboardingFailureStateMethodsDoNotRollbackForAdminException() throws Exception {
		assertThat(noRollbackFor(AdminOnboardingService.class, "temporaryLogin",
				String.class, String.class, String.class))
				.contains(AdminException.class);
		assertThat(noRollbackFor(AdminOnboardingService.class, "verifyOtp", String.class, String.class))
				.contains(AdminException.class);
	}

	private Class<?>[] noRollbackFor(Class<?> serviceType, String methodName, Class<?>... parameterTypes) throws Exception {
		Method method = serviceType.getMethod(methodName, parameterTypes);
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertThat(transactional).isNotNull();
		return Arrays.stream(transactional.noRollbackFor())
				.toArray(Class<?>[]::new);
	}
}
