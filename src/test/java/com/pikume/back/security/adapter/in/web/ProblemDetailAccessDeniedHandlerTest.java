package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import com.pikume.back.testsupport.NonUtf8DefaultEncodingMockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailAccessDeniedHandler")
class ProblemDetailAccessDeniedHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("권한 부족 시 403 Problem Details를 반환한다")
	void handleReturnsProblemDetails() throws Exception {
		ProblemDetailAccessDeniedHandler handler = new ProblemDetailAccessDeniedHandler(
				new SecurityProblemResponseWriter(
						new ObjectMapper(),
						new com.pikume.back.global.error.ProblemDetailFactory()));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");
		MockHttpServletResponse response = new NonUtf8DefaultEncodingMockHttpServletResponse();

		handler.handle(request, response, new AccessDeniedException("권한이 없습니다."));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getContentType()).contains("charset=UTF-8");
		assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
		assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(objectMapper.readTree(response.getContentAsString()).get("type").asText())
				.isEqualTo("https://api.pikume.com/problems/security/forbidden");
		assertThat(objectMapper.readTree(response.getContentAsString()).get("detail").asText())
				.isEqualTo("권한이 없습니다.");
		assertThat(objectMapper.readTree(response.getContentAsString()).get("instance").asText())
				.isEqualTo("/api/admin");
	}
}
