package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailAuthenticationEntryPoint")
class ProblemDetailAuthenticationEntryPointTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("인증 실패 시 401 Problem Details를 반환한다")
	void commenceReturnsProblemDetails() throws Exception {
		ProblemDetailAuthenticationEntryPoint entryPoint = new ProblemDetailAuthenticationEntryPoint(
				new ObjectMapper(),
				new com.pikume.back.global.error.ProblemDetailFactory());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(request, response, new BadCredentialsException("인증이 필요합니다."));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(objectMapper.readTree(response.getContentAsString()).get("type").asText())
				.isEqualTo("https://api.pikume.com/problems/security/unauthenticated");
		assertThat(objectMapper.readTree(response.getContentAsString()).get("instance").asText())
				.isEqualTo("/api/protected");
	}
}
