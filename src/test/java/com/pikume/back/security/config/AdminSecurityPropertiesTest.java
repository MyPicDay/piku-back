package com.pikume.back.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdminSecurityProperties")
class AdminSecurityPropertiesTest {

	@Test
	@DisplayName("운영 환경은 localhost Origin을 허용하지 않는다")
	void productionRejectsLocalhost() {
		AdminSecurityProperties properties = properties(
				List.of("http://localhost:3000"), "__Secure-pk-a91f", "__Secure-pk-b74d", true);

		assertThatThrownBy(properties::validateProduction)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("운영 환경은 Secure 속성과 __Secure 접두사를 강제한다")
	void productionRequiresSecurePrefixedCookies() {
		AdminSecurityProperties properties = properties(
				List.of("https://pikume-ops.pikume.com"), "pk-a91f", "pk-b74d", false);

		assertThatThrownBy(properties::validateProduction)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("운영 환경은 관리자 인증 용도를 드러내는 이름을 거부한다")
	void productionRejectsPurposeRevealingNames() {
		AdminSecurityProperties properties = properties(
				List.of("https://pikume-ops.pikume.com"),
				"__Secure-admin-session", "__Secure-pk-b74d", true);

		assertThatThrownBy(properties::validateProduction)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("세션 쿠키, CSRF 쿠키와 헤더 이름은 서로 달라야 한다")
	void rejectsDuplicateSecurityNames() {
		assertThatThrownBy(() -> new AdminSecurityProperties(
				List.of("http://localhost:3000"),
				"pk-a91f", "pk-a91f", "X-PK-C83F", false, ""))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("경로나 와일드카드가 포함된 Origin을 거부한다")
	void rejectsNonOriginValues() {
		assertThatThrownBy(() -> properties(
				List.of("https://*.pikume.com/admin"), "pk-a91f", "pk-b74d", false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("HTTP와 HTTPS가 아닌 Origin scheme을 거부한다")
	void rejectsUnsupportedOriginScheme() {
		assertThatThrownBy(() -> properties(
				List.of("ftp://pikume-ops.pikume.com"), "pk-a91f", "pk-b74d", false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("요청 Origin도 scheme, host, 기본 포트를 정규화해 비교한다")
	void normalizesRequestedOrigin() {
		AdminSecurityProperties properties = properties(
				List.of("https://pikume-ops.pikume.com"), "pk-a91f", "pk-b74d", false);

		assertThat(properties.allowsOrigin("HTTPS://PIKUME-OPS.PIKUME.COM:443")).isTrue();
	}

	private AdminSecurityProperties properties(List<String> origins, String sessionCookie,
			String csrfCookie, boolean secure) {
		return new AdminSecurityProperties(origins, sessionCookie, csrfCookie, "X-PK-C83F", secure, "pikume.com");
	}
}
