package com.pikume.back.global.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;
	private GlobalExceptionHandler exceptionHandler;
	private Level originalLoggerLevel;

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
	@DisplayName("필수 multipart 파트 누락은 validation Problem Details로 변환된다")
	void missingMultipartPartReturnsValidationProblem() throws Exception {
		mockMvc.perform(multipart("/test/multipart"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."))
				.andExpect(jsonPath("$.instance").value("/test/multipart"))
				.andExpect(jsonPath("$.fieldErrors.requiredPart").value("필수 요청 파트가 없습니다."));
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
	@DisplayName("validation 실패 로그는 거부된 요청 값을 기록하지 않는다")
	void validationFailureDoesNotLogRejectedValue() throws Exception {
		String sensitiveContent = "PRIVATE-DIARY-CONTENT";
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			mockMvc.perform(post("/test/validation")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"content\":\"" + sensitiveContent + "\"}"))
					.andExpect(status().isBadRequest());
		} finally {
			detachLogAppender(appender);
		}

		assertLogDoesNotContain(appender, sensitiveContent);
		assertDebugLogContains(appender,
				"event=request_validation_failed",
				"outcome=denied",
				"reason=invalid_request",
				"fieldCount=1");
	}

	@Test
	@DisplayName("constraint validation 실패 로그는 예외 메시지를 기록하지 않는다")
	void constraintViolationDoesNotLogExceptionMessage() {
		String sensitiveMessage = "PRIVATE-CONSTRAINT-VALUE";
		ConstraintViolationException exception = new ConstraintViolationException(sensitiveMessage, Set.of());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/constraint-validation");
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			ResponseEntity<ProblemDetail> response =
					exceptionHandler.handleConstraintViolationException(exception, request);
			assertThat(response.getStatusCode().value()).isEqualTo(400);
		} finally {
			detachLogAppender(appender);
		}

		assertLogDoesNotContain(appender, sensitiveMessage);
		assertDebugLogContains(appender,
				"event=request_validation_failed",
				"outcome=denied",
				"reason=constraint_violation",
				"fieldCount=0");
	}

	@Test
	@DisplayName("같은 경로의 constraint 위반이 여러 개여도 validation Problem Details를 반환한다")
	void duplicateConstraintViolationPathsReturnValidationProblem() {
		Path emailPath = mock(Path.class);
		given(emailPath.toString()).willReturn("email");
		ConstraintViolation<?> firstViolation = mock(ConstraintViolation.class);
		ConstraintViolation<?> secondViolation = mock(ConstraintViolation.class);
		given(firstViolation.getPropertyPath()).willReturn(emailPath);
		given(secondViolation.getPropertyPath()).willReturn(emailPath);
		given(firstViolation.getMessage()).willReturn("required");
		given(secondViolation.getMessage()).willReturn("format");
		ConstraintViolationException exception = new ConstraintViolationException(
				Set.of(firstViolation, secondViolation));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/constraint-validation");

		ResponseEntity<ProblemDetail> response =
				exceptionHandler.handleConstraintViolationException(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		Object fieldErrors = response.getBody().getProperties().get("fieldErrors");
		assertThat(fieldErrors).isInstanceOf(Map.class);
		Map<?, ?> fieldErrorMap = (Map<?, ?>) fieldErrors;
		assertThat(fieldErrorMap).hasSize(1);
		assertThat(fieldErrorMap.get("email")).isEqualTo("format, required");
	}

	@Test
	@DisplayName("method validation 실패 로그는 예외 메시지를 기록하지 않는다")
	void methodValidationDoesNotLogExceptionMessage() {
		String sensitiveMessage = "PRIVATE-METHOD-ARGUMENT";
		HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
		given(exception.getMessage()).willReturn(sensitiveMessage);
		given(exception.getParameterValidationResults()).willReturn(List.of());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/method-validation");
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			ResponseEntity<ProblemDetail> response =
					exceptionHandler.handleMethodValidationException(exception, request);
			assertThat(response.getStatusCode().value()).isEqualTo(400);
		} finally {
			detachLogAppender(appender);
		}

		assertLogDoesNotContain(appender, sensitiveMessage);
		assertDebugLogContains(appender,
				"event=request_validation_failed",
				"outcome=denied",
				"reason=invalid_method_argument",
				"fieldCount=0");
	}

	@Test
	@DisplayName("malformed body 실패 로그는 예외 메시지를 기록하지 않는다")
	void malformedBodyDoesNotLogExceptionMessage() {
		String sensitiveMessage = "PRIVATE-MALFORMED-BODY";
		HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
				sensitiveMessage, new MockHttpInputMessage(new byte[0]));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/validation");
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			ResponseEntity<ProblemDetail> response = exceptionHandler.handleHttpMessageNotReadable(exception, request);
			assertThat(response.getStatusCode().value()).isEqualTo(400);
		} finally {
			detachLogAppender(appender);
		}

		assertLogDoesNotContain(appender, sensitiveMessage);
		assertDebugLogContains(appender,
				"event=request_body_parse_failed",
				"outcome=denied",
				"reason=malformed_request");
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
	@DisplayName("IOException 실패 로그는 예외 메시지를 기록하지 않는다")
	void ioExceptionDoesNotLogExceptionMessage() {
		String sensitiveMessage = "PRIVATE-IO-CONTENT";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/files");
		IOException exception = new IOException(sensitiveMessage);
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			exceptionHandler.handleIOException(exception, request);
		} finally {
			detachLogAppender(appender);
		}

		assertLogDoesNotContain(appender, sensitiveMessage);
		assertErrorLogContains(appender,
				"event=request_failed",
				"outcome=failed",
				"reason=io_exception",
				"exception=IOException");
	}

	@Test
	@DisplayName("AsyncRequestNotUsableException 실패 로그는 개인정보와 예외 메시지를 기록하지 않는다")
	void asyncRequestNotUsableExceptionDoesNotLogSensitiveRequestData() {
		String sensitiveIp = "203.0.113.42";
		String sensitiveUserAgent = "PRIVATE-USER-AGENT";
		String sensitiveMessage = "PRIVATE-ASYNC-CONTENT";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sse/subscribe");
		request.addHeader("X-Forwarded-For", sensitiveIp);
		request.addHeader("User-Agent", sensitiveUserAgent);
		AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(sensitiveMessage);
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			exceptionHandler.handleAsyncRequestNotUsableException(exception, request);
		} finally {
			detachLogAppender(appender);
		}

		assertLogDoesNotContain(appender, sensitiveIp);
		assertLogDoesNotContain(appender, sensitiveUserAgent);
		assertLogDoesNotContain(appender, sensitiveMessage);
		assertErrorLogContains(appender,
				"event=request_failed",
				"outcome=failed",
				"reason=async_request_unusable",
				"exception=AsyncRequestNotUsableException");
	}

	private ListAppender<ILoggingEvent> attachLogAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		originalLoggerLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		logger.detachAppender(appender);
		logger.setLevel(originalLoggerLevel);
	}

	private void assertLogDoesNotContain(ListAppender<ILoggingEvent> appender, String forbiddenValue) {
		assertThat(appender.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.allSatisfy(message -> assertThat(message).doesNotContain(forbiddenValue));
	}

	private void assertDebugLogContains(ListAppender<ILoggingEvent> appender, String... expectedFragments) {
		assertThat(appender.list)
				.anySatisfy(event -> {
					assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
					assertThat(event.getFormattedMessage()).contains(expectedFragments);
				});
	}

	private void assertErrorLogContains(ListAppender<ILoggingEvent> appender, String... expectedFragments) {
		assertThat(appender.list)
				.anySatisfy(event -> {
					assertThat(event.getLevel()).isEqualTo(Level.ERROR);
					assertThat(event.getFormattedMessage()).contains(expectedFragments);
				});
	}

	@RestController
	static class TestController {

		@PostMapping("/test/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}

		@PostMapping(value = "/test/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		void multipart(@RequestPart("requiredPart") String requiredPart) {
		}
	}

	static class TestRequest {
		@NotBlank(message = "비어 있을 수 없습니다.")
		@Size(max = 4, message = "최대 4자까지 입력할 수 있습니다.")
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
