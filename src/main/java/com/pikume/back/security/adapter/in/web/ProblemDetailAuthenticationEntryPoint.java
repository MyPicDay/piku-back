package com.pikume.back.security.adapter.in.web;

import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemResponseWriter problemWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		problemWriter.write(
				request,
				response,
				SecurityProblemType.UNAUTHENTICATED,
				"인증이 필요합니다.");
	}
}
