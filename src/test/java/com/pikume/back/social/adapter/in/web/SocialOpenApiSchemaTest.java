package com.pikume.back.social.adapter.in.web;

import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.social.adapter.in.web.dto.CommentListResponseDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Social OpenAPI schema")
class SocialOpenApiSchemaTest {

	@Test
	@DisplayName("익명 응답에서 빠질 수 있는 작성자 메타데이터는 nullable이다")
	void anonymousCommentMetadataIsNullable() {
		Map<String, Schema> schemas = ModelConverters.getInstance().readAll(CommentListResponseDto.class);
		Schema<?> response = schemas.get("CommentListResponseDto");

		assertThat(response.getProperties().get("userId").getNullable()).isTrue();
		assertThat(response.getProperties().get("nickname").getNullable()).isTrue();
		assertThat(response.getProperties().get("avatar").getNullable()).isTrue();
	}

	@Test
	@DisplayName("친구 목록은 반환하지 않는 404와 204를 문서화하지 않는다")
	void friendPagesDoNotDocumentAbsentResponses() throws NoSuchMethodException {
		Method friendList = FriendController.class.getDeclaredMethod(
				"findFriendList", Pageable.class, UserPrincipal.class);
		Method requestList = FriendController.class.getDeclaredMethod(
				"findFriendRequests", Pageable.class, UserPrincipal.class);

		assertThat(responseCodes(friendList)).doesNotContain("404", "204");
		assertThat(responseCodes(requestList)).doesNotContain("204");
	}

	@Test
	@DisplayName("Social 오류 응답은 application/problem+json으로 문서화한다")
	void errorsUseProblemDetailsMediaType() throws NoSuchMethodException {
		Method addLike = LikeController.class.getDeclaredMethod("addLike", Long.class, UserPrincipal.class);
		Method deleteComment = CommentController.class.getDeclaredMethod(
				"deleteComment", Long.class, UserPrincipal.class);

		assertProblemMediaType(addLike, "404");
		assertProblemMediaType(addLike, "409");
		assertProblemMediaType(deleteComment, "400");
		assertProblemMediaType(deleteComment, "401");
		assertProblemMediaType(deleteComment, "404");
	}

	private String[] responseCodes(Method method) {
		Operation operation = method.getAnnotation(Operation.class);
		return java.util.stream.Stream.concat(
				Arrays.stream(method.getAnnotationsByType(ApiResponse.class)),
				operation == null ? java.util.stream.Stream.empty() : Arrays.stream(operation.responses()))
				.map(ApiResponse::responseCode)
				.toArray(String[]::new);
	}

	private void assertProblemMediaType(Method method, String responseCode) {
		ApiResponse response = Arrays.stream(method.getAnnotationsByType(ApiResponse.class))
				.filter(candidate -> candidate.responseCode().equals(responseCode))
				.findFirst()
				.orElseThrow();
		assertThat(response.content()).singleElement()
				.satisfies(content -> assertThat(content.mediaType())
						.isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
	}
}
