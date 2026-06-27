package com.pikume.back.global.config;

import com.pikume.back.security.config.AdminSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("개발 관리자 Swagger 보안 설정")
class DevAdminSwaggerSecurityConfigurationTest {

	@Test
	@DisplayName("HTTP Swagger는 host-only 쿠키를 사용하고 CSRF 쿠키를 요청 헤더로 전달한다")
	void configuresLocalSwaggerCookieAndCsrfHandling() throws IOException {
		MockEnvironment environment = loadDevelopmentEnvironment();

		AdminSecurityProperties admin = Binder.get(environment)
				.bind("admin.security", Bindable.of(AdminSecurityProperties.class))
				.get();
		SwaggerUiConfigProperties swagger = Binder.get(environment)
				.bind("springdoc.swagger-ui", Bindable.of(SwaggerUiConfigProperties.class))
				.get();

		assertThat(admin.secureCookies()).isFalse();
		assertThat(admin.csrfCookieDomain()).isEmpty();
		assertThat(swagger.getWithCredentials()).isTrue();
		assertThat(swagger.getCsrf().isEnabled()).isTrue();
		assertThat(swagger.getCsrf().getCookieName()).isEqualTo(admin.csrfCookieName());
		assertThat(swagger.getCsrf().getHeaderName()).isEqualTo(admin.csrfHeaderName());
	}

	private MockEnvironment loadDevelopmentEnvironment() throws IOException {
		MockEnvironment environment = new MockEnvironment();
		YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
		for (PropertySource<?> source : loader.load(
				"application", new FileSystemResource("src/main/resources/application.yml"))) {
			environment.getPropertySources().addLast(source);
		}
		for (PropertySource<?> source : loader.load(
				"application-dev", new FileSystemResource("src/main/resources/application-dev.yml"))) {
			environment.getPropertySources().addFirst(source);
		}
		return environment;
	}
}
