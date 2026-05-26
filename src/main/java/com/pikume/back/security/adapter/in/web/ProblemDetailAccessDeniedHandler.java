package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;
	private final ProblemDetailFactory problemDetailFactory;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException {
		ProblemDetail problemDetail = problemDetailFactory.create(
				SecurityProblemType.FORBIDDEN,
				accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "권한이 없습니다.",
				request.getRequestURI());

		response.setStatus(SecurityProblemType.FORBIDDEN.status().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), problemDetail);
	}
}
