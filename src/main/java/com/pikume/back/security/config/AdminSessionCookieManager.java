package com.pikume.back.security.config;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AdminSessionCookieManager {

	private final AdminSecurityProperties properties;

	public String requireSessionToken(HttpServletRequest request) {
		String value = cookieValue(request, properties.sessionCookieName());
		if (value == null || value.isBlank()) {
			throw new AdminException(AdminErrorCode.UNAUTHENTICATED, "관리자 세션이 필요합니다.");
		}
		return value;
	}

	public ResponseCookie sessionCookie(AdminSessionCredentialResult credentials, Duration maxAge) {
		return ResponseCookie.from(properties.sessionCookieName(), credentials.sessionToken())
				.httpOnly(true).secure(properties.secureCookies()).path("/api/admin")
				.maxAge(maxAge).sameSite("Strict").build();
	}

	public ResponseCookie csrfCookie(AdminSessionCredentialResult credentials, Duration maxAge) {
		ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie
				.from(properties.csrfCookieName(), credentials.csrfToken())
				.httpOnly(false).secure(properties.secureCookies()).path("/")
				.maxAge(maxAge).sameSite("Strict");
		if (properties.csrfCookieDomain() != null && !properties.csrfCookieDomain().isBlank()) {
			cookie.domain(properties.csrfCookieDomain());
		}
		return cookie.build();
	}

	public ResponseCookie expiredSessionCookie() {
		return ResponseCookie.from(properties.sessionCookieName(), "")
				.httpOnly(true).secure(properties.secureCookies()).path("/api/admin")
				.maxAge(Duration.ZERO).sameSite("Strict").build();
	}

	public ResponseCookie expiredCsrfCookie() {
		AdminSessionCredentialResult empty = new AdminSessionCredentialResult("", "");
		return csrfCookie(empty, Duration.ZERO);
	}

	private String cookieValue(HttpServletRequest request, String name) {
		return request.getCookies() == null ? null : Arrays.stream(request.getCookies())
				.filter(cookie -> name.equals(cookie.getName()))
				.map(Cookie::getValue).findFirst().orElse(null);
	}
}
