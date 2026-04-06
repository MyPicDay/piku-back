package com.pikume.back.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailFactory")
class ProblemDetailFactoryTest {

	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@Test
	@DisplayName("공통 problem type으로 RFC 9457 응답을 생성한다")
	void createProblemDetail() {
		ProblemDetail problemDetail = problemDetailFactory.create(
				CommonProblemType.RESOURCE_NOT_FOUND,
				"요청한 리소스를 찾을 수 없습니다.",
				"/api/test");

		assertThat(problemDetail.getType())
				.isEqualTo(URI.create("https://api.pikume.com/problems/common/resource-not-found"));
		assertThat(problemDetail.getTitle()).isEqualTo("Not Found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getDetail()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
		assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/api/test"));
	}

	@Test
	@DisplayName("validation 응답에는 fieldErrors 확장 필드가 포함된다")
	void createValidationProblemDetail() {
		ProblemDetail problemDetail = problemDetailFactory.validation(
				"요청 값이 올바르지 않습니다.",
				"/api/diary",
				Map.of("content", "비어 있을 수 없습니다."));

		assertThat(problemDetail.getType())
				.isEqualTo(URI.create("https://api.pikume.com/problems/validation/invalid-request"));
		assertThat(problemDetail.getTitle()).isEqualTo("Bad Request");
		assertThat(problemDetail.getStatus()).isEqualTo(400);
		assertThat(problemDetail.getDetail()).isEqualTo("요청 값이 올바르지 않습니다.");
		assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/api/diary"));
		assertThat(problemDetail.getProperties()).containsEntry("fieldErrors", Map.of("content", "비어 있을 수 없습니다."));
	}
}
