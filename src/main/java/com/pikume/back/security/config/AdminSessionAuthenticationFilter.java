package com.pikume.back.security.config;

import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.port.in.AdminSessionSecurityUseCase;
import com.pikume.back.admin.application.dto.AuthenticatedAdminSessionResult;
import com.pikume.back.security.adapter.in.web.SecurityProblemResponseWriter;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.security.principal.AdminPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AdminSessionAuthenticationFilter extends OncePerRequestFilter {

	private final AdminSecurityProperties properties;
	private final AdminSessionSecurityUseCase adminSessionSecurityUseCase;
	private final SecurityProblemResponseWriter problemWriter;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String rawSessionToken = sessionCookie(request);
		if (rawSessionToken == null) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			AuthenticatedAdminSessionResult session =
					adminSessionSecurityUseCase.authenticate(rawSessionToken, LocalDateTime.now());
			AdminPrincipal principal = new AdminPrincipal(
					session.adminId(), session.role(), session.sessionId());
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
			filterChain.doFilter(request, response);
		} catch (AdminException exception) {
			SecurityContextHolder.clearContext();
			if (exception.errorCode() == AdminErrorCode.SESSION_STORE_UNAVAILABLE) {
				problemWriter.write(
						request,
						response,
						SecurityProblemType.fromAdminErrorCode(exception.errorCode()),
						exception.getMessage());
				return;
			}
			filterChain.doFilter(request, response);
		}
	}

	private String sessionCookie(HttpServletRequest request) {
		return request.getCookies() == null ? null : Arrays.stream(request.getCookies())
				.filter(cookie -> properties.sessionCookieName().equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst().orElse(null);
	}
}
