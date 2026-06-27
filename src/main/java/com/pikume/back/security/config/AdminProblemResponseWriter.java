package com.pikume.back.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.error.ApiProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AdminProblemResponseWriter {

	private final ObjectMapper objectMapper;
	private final ProblemDetailFactory problemDetailFactory;

	public void write(HttpServletRequest request, HttpServletResponse response,
			ApiProblemType problem, String detail) throws IOException {
		ProblemDetail body = problemDetailFactory.create(problem, detail, request.getRequestURI());
		response.setStatus(problem.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setHeader("Cache-Control", "no-store");
		objectMapper.writeValue(response.getWriter(), body);
	}
}
