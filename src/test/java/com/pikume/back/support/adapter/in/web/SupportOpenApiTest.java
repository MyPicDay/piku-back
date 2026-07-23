package com.pikume.back.support.adapter.in.web;

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

@DisplayName("Support OpenAPI contract")
class SupportOpenApiTest {

	@Test
	@DisplayName("문의 API는 201 성공과 RFC 9457 오류 스키마를 명세한다")
	void documentsInquiryResponses() throws Exception {
		Method method = InquiryController.class.getDeclaredMethod(
				"saveInquiry",
				String.class,
				org.springframework.web.multipart.MultipartFile.class,
				com.pikume.back.global.config.CustomUserDetails.class);
		Map<String, ApiResponse> responses = Arrays.stream(method.getAnnotation(ApiResponses.class).value())
				.collect(Collectors.toMap(ApiResponse::responseCode, Function.identity()));

		assertThat(responses.keySet()).contains("201", "400", "404", "500");
		for (String status : java.util.List.of("400", "404", "500")) {
			assertThat(responses.get(status).content()[0].mediaType())
					.isEqualTo("application/problem+json");
			assertThat(responses.get(status).content()[0].schema().implementation())
					.isEqualTo(ProblemDetail.class);
		}
	}
}
