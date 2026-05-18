package com.pikume.back.global.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import com.pikume.back.global.error.ProblemDetailFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailFallbackExceptionResolver")
class ProblemDetailFallbackExceptionResolverTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("미처리 예외를 500 Problem Details로 반환한다")
	void resolveExceptionReturnsInternalServerErrorProblemDetail() throws Exception {
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				java.util.Optional.empty());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView modelAndView = resolver.resolveException(
				request,
				response,
				new Object(),
				new IllegalStateException("boom"));

		assertThat(modelAndView).isNotNull();
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getContentType()).startsWith("application/problem+json");

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(body.get("type").asText()).isEqualTo("https://api.pikume.com/problems/common/internal-server-error");
		assertThat(body.get("title").asText()).isEqualTo("Internal Server Error");
		assertThat(body.get("status").asInt()).isEqualTo(500);
		assertThat(body.get("detail").asText()).isEqualTo("서버에 오류가 발생했습니다.");
		assertThat(body.get("instance").asText()).isEqualTo("/api/test");
	}

	@Test
	@DisplayName("이미 커밋된 응답은 처리하지 않는다")
	void resolveExceptionDoesNotHandleCommittedResponse() throws Exception {
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				java.util.Optional.empty());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setCommitted(true);

		ModelAndView modelAndView = resolver.resolveException(
				request,
				response,
				new Object(),
				new IllegalStateException("boom"));

		assertThat(modelAndView).isNull();
		assertThat(response.getContentAsString()).isEmpty();
	}
}
