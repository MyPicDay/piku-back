package com.pikume.back.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminOriginValidationFilter extends OncePerRequestFilter {

	private static final String ADMIN_PATH_PREFIX = "/api/admin";
	private static final Set<String> ALLOWED_ORIGINS = Set.of("https://pikume-ops.pikume.com");

	private final ObjectMapper objectMapper;
	private final ProblemDetailFactory problemDetailFactory;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (requiresOriginValidation(request)) {
			String origin = request.getHeader(HttpHeaders.ORIGIN);
			if (!ALLOWED_ORIGINS.contains(origin)) {
				ProblemDetail problemDetail = problemDetailFactory.create(
						SecurityProblemType.ADMIN_ORIGIN_FORBIDDEN,
						"허용되지 않은 관리자 Origin입니다.",
						request.getRequestURI());
				response.setStatus(SecurityProblemType.ADMIN_ORIGIN_FORBIDDEN.status().value());
				response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
				response.setCharacterEncoding(StandardCharsets.UTF_8.name());
				objectMapper.writeValue(response.getWriter(), problemDetail);
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
