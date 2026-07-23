package com.pikume.back.security.adapter.in.web;

import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.security.adapter.in.web.dto.request.LoginRequest;
import com.pikume.back.security.adapter.in.web.dto.request.MobileLogoutRequest;
import com.pikume.back.security.adapter.in.web.dto.request.MobileReissueRequest;
import com.pikume.back.security.adapter.in.web.dto.response.LoginResponse;
import com.pikume.back.security.adapter.in.web.dto.response.MobileLoginResponse;
import com.pikume.back.security.adapter.in.web.dto.response.MobileReissueResponse;
import com.pikume.back.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security OpenAPI contract")
class SecurityOpenApiTest {

	@Test
	@DisplayName("Web 인증 API는 실제 성공 DTO와 RFC 9457 오류 스키마를 명세한다")
	void documentsWebAuthenticationResponses() throws Exception {
		assertResponses(
				AuthSessionController.class.getDeclaredMethod(
						"getCurrentUser",
						UserPrincipal.class,
						HttpServletRequest.class),
				LoginResponse.class,
				true);
		assertResponses(
				LoginController.class.getDeclaredMethod(
						"login",
						LoginRequest.class,
						HttpServletRequest.class),
				LoginResponse.class,
				true);
		assertResponses(
				LoginController.class.getDeclaredMethod(
						"reissue",
						HttpServletRequest.class),
				MessageResponse.class,
				true);
		assertResponses(
				LoginController.class.getDeclaredMethod(
						"logout",
						UserPrincipal.class,
						HttpServletRequest.class),
				MessageResponse.class,
				true);
	}

	@Test
	@DisplayName("모바일 인증 API는 Body Token 응답 DTO와 RFC 9457 오류 스키마를 명세한다")
	void documentsMobileAuthenticationResponses() throws Exception {
		assertResponses(
				MobileAuthController.class.getDeclaredMethod(
						"login",
						LoginRequest.class,
						HttpServletRequest.class),
				MobileLoginResponse.class,
				true);
		assertResponses(
				MobileAuthController.class.getDeclaredMethod(
						"reissue",
						MobileReissueRequest.class,
						HttpServletRequest.class),
				MobileReissueResponse.class,
				true);

		Method logout = MobileAuthController.class.getDeclaredMethod(
				"logout",
				MobileLogoutRequest.class,
				HttpServletRequest.class);
		ApiResponse response = logout.getAnnotation(ApiResponse.class);
		assertThat(response.responseCode()).isEqualTo("200");
		assertSuccessContent(response, MessageResponse.class);
	}

	private void assertResponses(Method method, Class<?> successType, boolean hasUnauthorizedResponse) {
		Map<String, ApiResponse> responses = Arrays.stream(method.getAnnotation(ApiResponses.class).value())
				.collect(Collectors.toMap(ApiResponse::responseCode, Function.identity()));

		assertSuccessContent(responses.get("200"), successType);
		if (hasUnauthorizedResponse) {
			ApiResponse unauthorized = responses.get("401");
			assertThat(unauthorized.content()).hasSize(1);
			assertThat(unauthorized.content()[0].mediaType()).isEqualTo("application/problem+json");
			assertThat(unauthorized.content()[0].schema().implementation()).isEqualTo(ProblemDetail.class);
		}
	}

	private void assertSuccessContent(ApiResponse response, Class<?> successType) {
		assertThat(response).isNotNull();
		assertThat(response.content()).hasSize(1);
		assertThat(response.content()[0].mediaType()).isEqualTo("application/json");
		assertThat(response.content()[0].schema().implementation()).isEqualTo(successType);
	}
}
