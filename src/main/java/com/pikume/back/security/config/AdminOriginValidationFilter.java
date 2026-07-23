package com.pikume.back.security.config;

import com.pikume.back.admin.application.port.in.RecordAdminSecurityEventUseCase;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AdminOriginValidationFilter extends OncePerRequestFilter {

	private static final String ADMIN_PATH_PREFIX = "/api/admin";
	private final AdminSecurityProperties properties;
	private final AdminProblemResponseWriter problemWriter;
	private final RecordAdminSecurityEventUseCase recordAdminSecurityEventUseCase;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (requiresOriginValidation(request)) {
			String origin = request.getHeader(HttpHeaders.ORIGIN);
			if (!properties.allowsOrigin(origin)) {
				recordAdminSecurityEventUseCase.recordOriginRejection();
				problemWriter.write(request, response, SecurityProblemType.ADMIN_ORIGIN_FORBIDDEN,
						"허용되지 않은 관리자 Origin입니다.");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean requiresOriginValidation(HttpServletRequest request) {
		return request.getRequestURI() != null
				&& request.getRequestURI().startsWith(ADMIN_PATH_PREFIX)
				&& !isSafeMethod(request.getMethod());
	}

	private boolean isSafeMethod(String method) {
		return "GET".equalsIgnoreCase(method)
				|| "HEAD".equalsIgnoreCase(method)
				|| "OPTIONS".equalsIgnoreCase(method);
	}
}
