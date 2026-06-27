package com.pikume.back.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "admin.security")
public record AdminSecurityProperties(
		List<String> allowedOrigins,
		String sessionCookieName,
		String csrfCookieName,
		String csrfHeaderName,
		boolean secureCookies,
		String csrfCookieDomain
) {

	public AdminSecurityProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
				.map(AdminSecurityProperties::normalizeOrigin)
				.distinct()
				.toList();
		if (allowedOrigins.isEmpty()) {
			throw new IllegalArgumentException("관리자 허용 Origin은 하나 이상이어야 합니다.");
		}
		requireName(sessionCookieName, "관리자 세션 쿠키 이름");
		requireName(csrfCookieName, "관리자 CSRF 쿠키 이름");
		requireName(csrfHeaderName, "관리자 CSRF 헤더 이름");
		if (sessionCookieName.equals(csrfCookieName)
				|| sessionCookieName.equals(csrfHeaderName)
				|| csrfCookieName.equals(csrfHeaderName)) {
			throw new IllegalArgumentException("관리자 보안 쿠키와 헤더 이름은 서로 달라야 합니다.");
		}
	}

	public void validateProduction() {
		if (!secureCookies) {
			throw new IllegalStateException("운영 관리자 쿠키는 Secure 속성이 필수입니다.");
		}
		if (!sessionCookieName.startsWith("__Secure-") || !csrfCookieName.startsWith("__Secure-")) {
			throw new IllegalStateException("운영 관리자 쿠키 이름은 __Secure- 접두사가 필수입니다.");
		}
		if (allowedOrigins.stream().anyMatch(origin -> origin.contains("localhost") || !origin.startsWith("https://"))) {
			throw new IllegalStateException("운영 관리자 Origin은 HTTPS 원격 Origin만 허용합니다.");
		}
		if (csrfCookieDomain == null || csrfCookieDomain.isBlank()) {
			throw new IllegalStateException("운영 관리자 CSRF 쿠키 도메인은 필수입니다.");
		}
		if (revealsPurpose(sessionCookieName) || revealsPurpose(csrfCookieName) || revealsPurpose(csrfHeaderName)) {
			throw new IllegalStateException("운영 관리자 보안 이름은 용도를 드러내면 안 됩니다.");
		}
	}

	public boolean allowsOrigin(String origin) {
		try {
			return allowedOrigins.contains(normalizeOrigin(origin));
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private boolean revealsPurpose(String value) {
		String normalized = value.toLowerCase();
		return normalized.contains("admin") || normalized.contains("auth")
				|| normalized.contains("session") || normalized.contains("csrf");
	}

	private static String normalizeOrigin(String value) {
		try {
			URI uri = URI.create(value == null ? "" : value.trim());
			if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					|| uri.getHost() == null || uri.getUserInfo() != null
					|| uri.getPath() != null && !uri.getPath().isEmpty()
					|| uri.getQuery() != null || uri.getFragment() != null || value.contains("*")) {
				throw new IllegalArgumentException();
			}
			String scheme = uri.getScheme().toLowerCase();
			boolean defaultPort = "http".equals(scheme) && uri.getPort() == 80
					|| "https".equals(scheme) && uri.getPort() == 443;
			String authority = uri.getPort() < 0 || defaultPort
					? uri.getHost()
					: uri.getHost() + ":" + uri.getPort();
			return scheme + "://" + authority.toLowerCase();
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("유효하지 않은 관리자 Origin입니다: " + value, exception);
		}
	}

	private static void requireName(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + "은 필수입니다.");
		}
	}
}
