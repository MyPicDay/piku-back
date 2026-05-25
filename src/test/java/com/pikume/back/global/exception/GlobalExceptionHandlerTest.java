package com.pikume.back.global.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;
	private GlobalExceptionHandler exceptionHandler;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		exceptionHandler = new GlobalExceptionHandler(
				Optional.empty(),
				new com.pikume.back.global.error.ProblemDetailFactory());
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(exceptionHandler)
				.setValidator(validator)
				.build();
	}

	@Test
	@DisplayName("validation 실패는 validation type과 fieldErrors를 반환한다")
	void validationFailure() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.fieldErrors.content").exists());
	}

	@Test
	@DisplayName("malformed body는 common malformed-request로 변환된다")
	void malformedBody() throws Exception {
		mockMvc.perform(post("/test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/common/malformed-request"))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("클라이언트 연결 중단 IOException은 정상 종료로 처리한다")
	void clientConnectionAbortIOExceptionReturnsNoContent() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sse/subscribe");
		IOException exception = new IOException("현재 연결은 사용자의 호스트 시스템의 소프트웨어에 의해 중단되었습니다");

		ResponseEntity<ProblemDetail> response = exceptionHandler.handleIOException(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		assertThat(response.getBody()).isNull();
	}

	@Test
	@DisplayName("클라이언트 연결 중단이 아닌 AsyncRequestNotUsableException은 error로 기록한다")
	void asyncRequestNotUsableExceptionWithoutClientDisconnectLogsError() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sse/subscribe");
		AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException("async response failed");
		Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			exceptionHandler.handleAsyncRequestNotUsableException(exception, request);
		} finally {
			logger.detachAppender(appender);
		}

		assertThat(appender.list)
				.anySatisfy(event -> {
					assertThat(event.getLevel()).isEqualTo(Level.ERROR);
					assertThat(event.getFormattedMessage()).contains("비동기 요청을 사용할 수 없습니다");
				});
	}

	@RestController
	static class TestController {

		@PostMapping("/test/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}
	}

	static class TestRequest {
		@NotBlank(message = "비어 있을 수 없습니다.")
		private final String content;

		@JsonCreator
		TestRequest(String content) {
			this.content = content;
		}

		public String getContent() {
			return content;
		}
	}
}
