package com.pikume.back.global.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.notification.DiscordWebhookService;
import com.pikume.back.testsupport.NonUtf8DefaultEncodingMockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("ProblemDetailFallbackExceptionResolver")
class ProblemDetailFallbackExceptionResolverTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("미처리 예외를 500 Problem Details로 반환한다")
	void resolveExceptionReturnsInternalServerErrorProblemDetail() throws Exception {
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				Optional.empty());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		MockHttpServletResponse response = new NonUtf8DefaultEncodingMockHttpServletResponse();

		ModelAndView modelAndView = resolver.resolveException(
				request,
				response,
				new Object(),
				new IllegalStateException("boom"));

		assertThat(modelAndView).isNotNull();
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getContentType()).contains("charset=UTF-8");
		assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(body.get("type").asText()).isEqualTo("https://api.pikume.com/problems/common/internal-server-error");
		assertThat(body.get("title").asText()).isEqualTo("Internal Server Error");
		assertThat(body.get("status").asInt()).isEqualTo(500);
		assertThat(body.get("detail").asText()).isEqualTo("서버에 오류가 발생했습니다.");
		assertThat(body.get("instance").asText()).isEqualTo("/api/test");
	}

	@Test
	@DisplayName("미처리 예외 로그는 예외 메시지를 기록하지 않는다")
	void unresolvedExceptionDoesNotLogExceptionMessage() {
		String sensitiveMessage = "PRIVATE-UNHANDLED-CONTENT";
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				Optional.empty());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		Logger logger = (Logger) LoggerFactory.getLogger(ProblemDetailFallbackExceptionResolver.class);
		Level originalLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			resolver.resolveException(request, response, new Object(), new IllegalStateException(sensitiveMessage));
		} finally {
			logger.detachAppender(appender);
			logger.setLevel(originalLevel);
		}

		assertThat(appender.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.allSatisfy(message -> assertThat(message).doesNotContain(sensitiveMessage));
		assertThat(appender.list)
				.anySatisfy(event -> assertThat(event.getFormattedMessage()).contains(
						"event=request_failed",
						"outcome=failed",
						"reason=unhandled_exception",
						"exception=IllegalStateException"));
	}

	@Test
	@DisplayName("이미 커밋된 응답은 처리하지 않는다")
	void resolveExceptionDoesNotHandleCommittedResponse() throws Exception {
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				Optional.empty());
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

	@Test
	@DisplayName("미처리 예외는 운영 알림에 전달하고 공통 500 Problem Details로 반환한다")
	void unresolvedExceptionReportsOperationalAlert() throws Exception {
		DiscordWebhookService discordWebhookService = mock(DiscordWebhookService.class);
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				Optional.of(discordWebhookService));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/comments");
		MockHttpServletResponse response = new NonUtf8DefaultEncodingMockHttpServletResponse();
		IllegalStateException failure = new IllegalStateException("storage unavailable");

		ModelAndView modelAndView = resolver.resolveException(
				request,
				response,
				new Object(),
				failure);

		assertThat(modelAndView).isNotNull();
		assertThat(response.getStatus()).isEqualTo(500);
		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(body.get("type").asText())
				.isEqualTo("https://api.pikume.com/problems/common/internal-server-error");
		then(discordWebhookService).should().sendExceptionNotification(failure, request);
	}

	@Test
	@DisplayName("운영 알림 요청 실패는 공통 500 Problem Details 응답을 막지 않는다")
	void operationalAlertFailureDoesNotPreventFallbackResponse() throws Exception {
		DiscordWebhookService discordWebhookService = mock(DiscordWebhookService.class);
		ProblemDetailFallbackExceptionResolver resolver = new ProblemDetailFallbackExceptionResolver(
				objectMapper,
				new ProblemDetailFactory(),
				Optional.of(discordWebhookService));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/comments");
		MockHttpServletResponse response = new NonUtf8DefaultEncodingMockHttpServletResponse();
		IllegalStateException failure = new IllegalStateException("storage unavailable");
		willThrow(new IllegalStateException("notification unavailable"))
				.given(discordWebhookService)
				.sendExceptionNotification(failure, request);

		ModelAndView modelAndView = resolver.resolveException(
				request,
				response,
				new Object(),
				failure);

		assertThat(modelAndView).isNotNull();
		assertThat(response.getStatus()).isEqualTo(500);
		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(body.get("type").asText())
				.isEqualTo("https://api.pikume.com/problems/common/internal-server-error");
	}
}
