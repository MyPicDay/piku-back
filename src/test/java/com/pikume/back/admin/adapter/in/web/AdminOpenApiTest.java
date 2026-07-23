package com.pikume.back.admin.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Admin OpenAPI contract")
class AdminOpenApiTest {

	@Test
	@DisplayName("관리자 Controller는 실제 오류 범위를 RFC 9457 스키마로 명세한다")
	void documentsProblemDetailErrorsByApiCategory() {
		Map<Class<?>, Set<String>> expectedStatuses = Map.of(
				AdminAccountController.class, Set.of("400", "401", "403", "404", "409", "503"),
				AdminAuthController.class, Set.of("400", "401", "403", "423", "429", "503"),
				AdminOnboardingController.class, Set.of("400", "401", "403", "409", "423", "429", "503"),
				AdminCsrfController.class, Set.of("403", "503"),
				AdminAuditLogController.class, Set.of("401", "403", "503"),
				AdminDashboardController.class, Set.of("401", "403", "503"),
				AdminStatisticsController.class, Set.of("400", "401", "403", "503"));

		expectedStatuses.forEach((controller, statuses) -> {
			ApiResponses annotation = controller.getAnnotation(ApiResponses.class);
			assertThat(annotation)
					.as("%s의 ApiResponses", controller.getSimpleName())
					.isNotNull();
			Map<String, ApiResponse> responses = Arrays.stream(annotation.value())
					.collect(Collectors.toMap(ApiResponse::responseCode, Function.identity()));
			assertThat(responses.keySet()).containsExactlyInAnyOrderElementsOf(statuses);
			for (String status : statuses) {
				assertThat(List.of(responses.get(status).content()))
						.as("%s %s content", controller.getSimpleName(), status)
						.hasSize(1);
				assertThat(responses.get(status).content()[0].mediaType())
						.isEqualTo("application/problem+json");
				assertThat(responses.get(status).content()[0].schema().implementation())
						.isEqualTo(ProblemDetail.class);
			}
		});
	}

	@Test
	@DisplayName("명시적 201과 204 성공 상태가 실제 Controller 응답과 일치한다")
	void documentsNonDefaultSuccessStatuses() throws Exception {
		Method createAccount = AdminAccountController.class.getDeclaredMethod(
				"create",
				com.pikume.back.security.principal.AdminPrincipal.class,
				com.pikume.back.admin.adapter.in.web.dto.request.CreateAdminAccountRequest.class);
		Method initializeCsrf = AdminCsrfController.class.getDeclaredMethod("initialize");

		assertThat(createAccount.getAnnotation(ApiResponse.class).responseCode()).isEqualTo("201");
		assertThat(initializeCsrf.getAnnotation(ApiResponse.class).responseCode()).isEqualTo("204");
	}
}
