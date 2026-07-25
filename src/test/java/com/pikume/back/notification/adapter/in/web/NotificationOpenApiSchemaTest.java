package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.notification.adapter.in.web.dto.NotificationPageResponse;
import com.pikume.back.security.principal.UserPrincipal;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification OpenAPI schema")
class NotificationOpenApiSchemaTest {

	@Test
	@DisplayName("Notification Page content는 NotificationResponse 배열로 문서화한다")
	void pageContentReferencesNotificationResponse() {
		Map<String, Schema> schemas = ModelConverters.getInstance().readAll(NotificationPageResponse.class);
		Schema<?> pageSchema = schemas.get("NotificationPageResponse");

		assertThat(pageSchema).isNotNull();
		assertThat(pageSchema.getProperties().get("content"))
				.isInstanceOfSatisfying(ArraySchema.class,
						content -> assertThat(content.getItems().get$ref())
								.endsWith("/NotificationResponse"));
	}

	@Test
	@DisplayName("메타데이터 부재가 가능한 응답 필드는 nullable로 문서화한다")
	void optionalMetadataIsNullable() {
		Map<String, Schema> schemas = ModelConverters.getInstance().readAll(NotificationPageResponse.class);
		Schema<?> responseSchema = schemas.get("NotificationResponse");

		assertThat(responseSchema).isNotNull();
		assertThat(responseSchema.getProperties().get("nickname").getNullable()).isTrue();
		assertThat(responseSchema.getProperties().get("avatarUrl").getNullable()).isTrue();
		assertThat(responseSchema.getProperties().get("relatedDiaryId").getNullable()).isTrue();
		assertThat(responseSchema.getProperties().get("thumbnailUrl").getNullable()).isTrue();
		assertThat(responseSchema.getProperties().get("diaryDate").getNullable()).isTrue();
		assertThat(responseSchema.getProperties().get("diaryUserId").getNullable()).isTrue();
	}

	@Test
	@DisplayName("읽음은 멱등 204로, 삭제의 404는 Problem Details로 문서화한다")
	void commandResponsesMatchRuntimeContracts() throws NoSuchMethodException {
		assertNoContentOnly(NotificationController.class.getDeclaredMethod(
				"markAsRead",
				Long.class,
				UserPrincipal.class));
		assertProblemDetails404(NotificationController.class.getDeclaredMethod(
				"deleteNotification",
				Long.class,
				UserPrincipal.class));
	}

	private void assertNoContentOnly(Method method) {
		ApiResponses responses = method.getAnnotation(ApiResponses.class);

		assertThat(Arrays.stream(responses.value())
				.map(ApiResponse::responseCode))
				.containsExactly("204");
	}

	private void assertProblemDetails404(Method method) {
		ApiResponses responses = method.getAnnotation(ApiResponses.class);
		ApiResponse notFound = Arrays.stream(responses.value())
				.filter(response -> response.responseCode().equals("404"))
				.findFirst()
				.orElseThrow();

		assertThat(notFound.content()).singleElement().satisfies(content -> {
			assertThat(content.mediaType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			assertThat(content.schema().implementation()).isEqualTo(ProblemDetail.class);
		});
	}
}
