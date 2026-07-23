package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.global.error.ProblemDetailFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreativeExceptionHandler")
class CreativeExceptionHandlerTest {

	private final CreativeExceptionHandler handler =
			new CreativeExceptionHandler(new ProblemDetailFactory());

	@Test
	@DisplayName("일일 생성 한도 초과를 RFC 9457 응답으로 변환한다")
	void mapsQuotaExceededToProblemDetails() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/diary/ai/generate");

		ResponseEntity<ProblemDetail> response =
				handler.handleCreativeException(new AiGenerationQuotaExceededException(5), request);

		assertThat(response.getStatusCode().value()).isEqualTo(429);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getType().toString())
				.isEqualTo("https://api.pikume.com/problems/common/rate-limit-exceeded");
		assertThat(response.getBody().getDetail()).isEqualTo("일일 생성 횟수(5회)를 모두 사용하셨습니다.");
		assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/diary/ai/generate");
	}

	@Test
	@DisplayName("Provider 내부 원인을 이미지 생성 실패 응답에 노출하지 않는다")
	void hidesProviderFailureDetails() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/diary/ai/generate");
		CreativeException exception = new CreativeException(
				CreativeErrorCode.IMAGE_GENERATION_FAILED,
				new IllegalStateException("provider secret response"));

		ResponseEntity<ProblemDetail> response = handler.handleCreativeException(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDetail()).isEqualTo("AI 이미지 생성에 실패했습니다.");
		assertThat(response.getBody().getDetail()).doesNotContain("provider secret response");
	}
}
