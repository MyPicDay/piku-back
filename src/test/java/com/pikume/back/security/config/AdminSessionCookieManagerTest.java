package com.pikume.back.security.config;

import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdminSessionCookieManager")
class AdminSessionCookieManagerTest {

	@Test
	@DisplayName("운영 세션 쿠키는 host-only, HttpOnly, Secure, SameSite Strict이다")
	void createsProductionSessionCookie() {
		String cookie = manager().sessionCookie(credentials(), Duration.ofHours(8)).toString();

		assertThat(cookie)
				.startsWith("__Secure-pk-a91f=raw-session")
				.contains("Path=/api/admin", "Secure", "HttpOnly", "SameSite=Strict")
				.doesNotContain("Domain=");
	}

	@Test
	@DisplayName("운영 CSRF 쿠키는 부모 도메인에서 읽을 수 있고 HttpOnly가 아니다")
	void createsProductionCsrfCookie() {
		String cookie = manager().csrfCookie(credentials(), Duration.ofHours(8)).toString();

		assertThat(cookie)
				.startsWith("__Secure-pk-b74d=raw-csrf")
				.contains("Path=/", "Domain=pikume.com", "Secure", "SameSite=Strict")
				.doesNotContain("HttpOnly");
	}

	private AdminSessionCookieManager manager() {
		return new AdminSessionCookieManager(new AdminSecurityProperties(
				List.of("https://pikume-ops.pikume.com"),
				"__Secure-pk-a91f", "__Secure-pk-b74d", "X-PK-C83F", true, "pikume.com"));
	}

	private AdminSessionCredentialResult credentials() {
		return new AdminSessionCredentialResult("raw-session", "raw-csrf");
	}
}
