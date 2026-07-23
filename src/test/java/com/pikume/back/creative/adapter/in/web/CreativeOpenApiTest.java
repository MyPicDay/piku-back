package com.pikume.back.creative.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Creative OpenAPI contract")
class CreativeOpenApiTest {

	@Test
	@DisplayName("AI 이미지 생성 API는 성공 응답과 RFC 9457 오류 스키마를 명세한다")
	void documentsGenerateImageResponses() throws Exception {
		Method method = AiGeneratorController.class.getDeclaredMethod(
				"generateDiaryImage",
				com.pikume.back.creative.adapter.in.web.dto.GenerateDiaryImageRequest.class,
				com.pikume.back.security.principal.UserPrincipal.class);
		Map<String, ApiResponse> responses = Arrays.stream(method.getAnnotation(ApiResponses.class).value())
				.collect(Collectors.toMap(ApiResponse::responseCode, Function.identity()));

		assertThat(responses.keySet()).contains("200", "400", "429", "500");
		for (String status : java.util.List.of("400", "429", "500")) {
			assertThat(responses.get(status).content()[0].mediaType())
					.isEqualTo("application/problem+json");
			assertThat(responses.get(status).content()[0].schema().implementation())
					.isEqualTo(ProblemDetail.class);
		}
	}
}
