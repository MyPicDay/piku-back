package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.testsupport.NonUtf8DefaultEncodingMockHttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security Problem Details writer")
class SecurityProblemResponseWriterTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SecurityProblemResponseWriter writer = new SecurityProblemResponseWriter(
			objectMapper,
			new ProblemDetailFactory());

	@Test
	@DisplayName("RFC 9457 필드와 보안 캐시 헤더를 기록한다")
	void writesProblemDetailsWithNoStore() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/reports");
		MockHttpServletResponse response = new NonUtf8DefaultEncodingMockHttpServletResponse();

		writer.write(
				request,
				response,
				SecurityProblemType.ADMIN_CSRF_INVALID,
				"관리자 CSRF 토큰이 유효하지 않습니다.");

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
		assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(body.get("type").asText())
				.isEqualTo("https://api.pikume.com/problems/admin/csrf-invalid");
		assertThat(body.get("title").asText()).isEqualTo("Forbidden");
		assertThat(body.get("status").asInt()).isEqualTo(403);
		assertThat(body.get("detail").asText())
				.isEqualTo("관리자 CSRF 토큰이 유효하지 않습니다.");
		assertThat(body.get("instance").asText()).isEqualTo("/api/admin/reports");
	}

	@Test
	@DisplayName("이미 커밋된 응답에는 다시 쓰지 않는다")
	void doesNotWriteCommittedResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setCommitted(true);

		writer.write(
				request,
				response,
				SecurityProblemType.UNAUTHENTICATED,
				"인증이 필요합니다.");

		assertThat(response.getContentAsByteArray()).isEmpty();
		assertThat(response.getContentType()).isNull();
	}
}
