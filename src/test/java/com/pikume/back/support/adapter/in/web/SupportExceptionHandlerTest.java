package com.pikume.back.support.adapter.in.web;

import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.support.application.exception.SupportErrorCode;
import com.pikume.back.support.application.exception.SupportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SupportExceptionHandler")
class SupportExceptionHandlerTest {

	private final SupportExceptionHandler handler =
			new SupportExceptionHandler(new ProblemDetailFactory());

	@Test
	@DisplayName("존재하지 않는 제출자를 RFC 9457 404 응답으로 변환한다")
	void mapsMissingSubmitterToProblemDetails() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/inquiry");

		ResponseEntity<ProblemDetail> response = handler.handleSupportException(
				new SupportException(SupportErrorCode.SUBMITTER_NOT_FOUND),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getType().toString())
				.isEqualTo("https://api.pikume.com/problems/support/submitter-not-found");
		assertThat(response.getBody().getDetail())
				.isEqualTo("문의 제출 사용자를 찾을 수 없습니다.");
		assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/inquiry");
	}

	@Test
	@DisplayName("Storage 내부 원인을 첨부 저장 실패 응답에 노출하지 않는다")
	void hidesStorageFailureDetails() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/inquiry");

		ResponseEntity<ProblemDetail> response = handler.handleSupportException(
				new SupportException(
						SupportErrorCode.ATTACHMENT_STORAGE_FAILED,
						new IllegalStateException("bucket secret")),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDetail())
				.isEqualTo("문의 첨부 이미지를 저장할 수 없습니다.")
				.doesNotContain("bucket secret");
	}

	@Test
	@DisplayName("필수 Multipart 누락을 RFC 9457 400 응답으로 변환한다")
	void mapsMissingMultipartToProblemDetails() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/inquiry");

		ResponseEntity<ProblemDetail> response = handler.handleMissingRequestPart(
				new MissingServletRequestPartException("content"),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDetail())
				.isEqualTo("필수 문의 요청 값이 없습니다: content");
		assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/inquiry");
	}
}
