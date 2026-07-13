package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.port.in.AdminAccountOperationUseCase;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.port.in.CreateAdminAccountUseCase;
import com.pikume.back.security.config.AdminSessionCookieManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("관리자 API 노출 설정")
class AdminApiExposureConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(CreateAdminAccountUseCase.class, () -> mock(CreateAdminAccountUseCase.class))
			.withBean(AdminAccountOperationUseCase.class, () -> mock(AdminAccountOperationUseCase.class))
			.withBean(AdminOnboardingUseCase.class, () -> mock(AdminOnboardingUseCase.class))
			.withBean(AdminSessionCookieManager.class, () -> mock(AdminSessionCookieManager.class))
			.withUserConfiguration(TestConfiguration.class);

	@Test
	@DisplayName("기능 플래그가 없으면 계정 관리와 온보딩 API를 등록하지 않는다")
	void disablesAdminApisByDefault() {
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(AdminAccountController.class);
			assertThat(context).doesNotHaveBean(AdminOnboardingController.class);
		});
	}

	@Test
	@DisplayName("계정 관리 기능만 활성화하면 계정 관리 API만 등록한다")
	void enablesOnlyAdminAccountApi() {
		contextRunner
				.withPropertyValues("admin.api.account-management-enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(AdminAccountController.class);
					assertThat(context).doesNotHaveBean(AdminOnboardingController.class);
				});
	}

	@Test
	@DisplayName("온보딩 기능만 활성화하면 온보딩 API만 등록한다")
	void enablesOnlyAdminOnboardingApi() {
		contextRunner
				.withPropertyValues("admin.api.onboarding-enabled=true")
				.run(context -> {
					assertThat(context).doesNotHaveBean(AdminAccountController.class);
					assertThat(context).hasSingleBean(AdminOnboardingController.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	@Import({AdminAccountController.class, AdminOnboardingController.class})
	static class TestConfiguration {
	}
}
