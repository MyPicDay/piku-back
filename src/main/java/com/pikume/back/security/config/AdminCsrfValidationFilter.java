package com.pikume.back.security.config;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminSessionSecurityUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AdminCsrfValidationFilter extends OncePerRequestFilter {

	private final AdminSecurityProperties properties;
	private final AdminSessionSecurityUseCase adminSessionSecurityUseCase;
	private final AdminProblemResponseWriter problemWriter;
	private final AdminSessionTelemetryPort telemetryPort;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!requiresValidation(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		String sessionToken = cookie(request, properties.sessionCookieName());
		if (sessionToken == null) {
			telemetryPort.csrfRejected("session_missing");
			problemWriter.write(request, response, AdminProblem.UNAUTHENTICATED, "관리자 세션이 필요합니다.");
			return;
		}
		String csrfCookie = cookie(request, properties.csrfCookieName());
		String csrfHeader = request.getHeader(properties.csrfHeaderName());
		if (!constantTimeEquals(csrfCookie, csrfHeader)) {
			telemetryPort.csrfRejected("cookie_header_mismatch");
			problemWriter.write(request, response, AdminProblem.CSRF_INVALID, "관리자 CSRF 토큰이 유효하지 않습니다.");
			return;
		}
		try {
			adminSessionSecurityUseCase.validateCsrf(sessionToken, csrfHeader, LocalDateTime.now());
			filterChain.doFilter(request, response);
		} catch (AdminException exception) {
			telemetryPort.csrfRejected("server_session_mismatch");
			problemWriter.write(request, response, exception.problem(), exception.getMessage());
		}
	}

	private boolean requiresValidation(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.startsWith("/api/admin")
				&& !uri.equals("/api/admin/auth/csrf")
				&& !isSafe(request.getMethod());
	}

	private boolean isSafe(String method) {
		return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
	}

	private String cookie(HttpServletRequest request, String name) {
		return request.getCookies() == null ? null : Arrays.stream(request.getCookies())
				.filter(cookie -> name.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst().orElse(null);
	}

	private boolean constantTimeEquals(String left, String right) {
		if (left == null || right == null) {
			return false;
		}
		return MessageDigest.isEqual(
				left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
	}
}
