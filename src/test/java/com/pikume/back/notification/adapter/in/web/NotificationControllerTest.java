package com.pikume.back.notification.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.port.in.DeleteNotificationUseCase;
import com.pikume.back.notification.application.port.in.MarkAllNotificationsReadUseCase;
import com.pikume.back.notification.application.port.in.MarkNotificationReadUseCase;
import com.pikume.back.notification.application.port.in.QueryNotificationPageUseCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

	@Mock
	private QueryNotificationPageUseCase queryNotificationPageUseCase;

	@Mock
	private MarkNotificationReadUseCase markNotificationReadUseCase;
	@Mock
	private MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
	@Mock
	private DeleteNotificationUseCase deleteNotificationUseCase;

	private NotificationController notificationController;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		notificationController = new NotificationController(
				queryNotificationPageUseCase,
				markNotificationReadUseCase,
				markAllNotificationsReadUseCase,
				deleteNotificationUseCase,
				new NotificationWebMapper(),
				new ProblemDetailFactory());
		mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
				.setCustomArgumentResolvers(
						principalResolver(),
						new PageableHandlerMethodArgumentResolver())
				.build();
	}

	@Test
	@DisplayName("PATCH /api/sse/{notificationId}는 대상이 없어도 멱등적으로 성공한다")
	void markAsReadIsIdempotentWhenNotificationDoesNotExist() {
		ResponseEntity<Void> response = notificationController.markAsRead(
				1L,
				new UserPrincipal("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		assertThat(response.getBody()).isNull();
		then(markNotificationReadUseCase).should().markNotificationRead(1L, "user1");
	}

	@Test
	@DisplayName("DELETE /api/sse/{notificationId}는 알림이 없으면 Problem Details를 반환한다")
	void deleteNotificationReturnsProblemDetailWhenNotificationDoesNotExist() {
		given(deleteNotificationUseCase.deleteNotification(1L, "user1")).willReturn(false);

		ResponseEntity<?> response = notificationController.deleteNotification(
				1L,
				new UserPrincipal("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getTitle()).isEqualTo("Not Found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getDetail()).isEqualTo("알림을 찾을 수 없습니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/sse/1");
	}

	@Test
	@DisplayName("GET /api/sse/notifications는 content와 전체 Page 메타데이터를 반환한다")
	void getNotificationsPreservesPageJsonContract() {
		NotificationResult item = new NotificationResult(
				1L,
				"님이 일기에 댓글을 달았습니다.",
				"보낸이",
				null,
				NotificationKind.COMMENT,
				10L,
				null,
				false,
				null,
				null,
				"sender-id");
		given(queryNotificationPageUseCase.queryNotifications(eq("user1"), any()))
				.willReturn(new PageResult<>(List.of(item), 1, 2, 5));

		ResponseEntity<?> response = notificationController.getNotifications(
				new UserPrincipal("user1", "pikume"),
				PageRequest.of(1, 2));

		JsonNode json = new ObjectMapper().valueToTree(response.getBody());
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(json.path("content").size()).isEqualTo(1);
		assertThat(json.path("content").get(0).path("id").asLong()).isEqualTo(1L);
		assertThat(json.path("content").get(0).path("type").asText()).isEqualTo("COMMENT");
		assertThat(json.path("number").asInt()).isEqualTo(1);
		assertThat(json.path("size").asInt()).isEqualTo(2);
		assertThat(json.path("totalElements").asLong()).isEqualTo(5L);
		assertThat(json.path("totalPages").asInt()).isEqualTo(3);
		assertThat(json.path("numberOfElements").asInt()).isEqualTo(1);
		assertThat(json.path("first").asBoolean()).isFalse();
		assertThat(json.path("last").asBoolean()).isFalse();
		assertThat(json.path("empty").asBoolean()).isFalse();
		assertThat(json.path("sort").path("sorted").asBoolean()).isTrue();
		assertThat(json.path("sort").path("unsorted").asBoolean()).isFalse();
		assertThat(json.path("sort").path("empty").asBoolean()).isFalse();
		assertThat(json.path("pageable").path("pageNumber").asInt()).isEqualTo(1);
		assertThat(json.path("pageable").path("pageSize").asInt()).isEqualTo(2);
		assertThat(json.path("pageable").path("offset").asLong()).isEqualTo(2L);
		assertThat(json.path("pageable").path("paged").asBoolean()).isTrue();
		assertThat(json.path("pageable").path("unpaged").asBoolean()).isFalse();
		assertThat(json.path("pageable").path("sort").path("sorted").asBoolean()).isTrue();
	}

	@Test
	@DisplayName("MVC에서 읽을 활성 알림이 없어도 204를 반환한다")
	void mvcReturnsNoContentWhenActiveNotificationDoesNotExist() throws Exception {
		mockMvc.perform(patch("/api/sse/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("MVC에서 삭제할 활성 알림이 없으면 RFC 9457 404를 반환한다")
	void mvcDeleteReturnsProblemDetailsWhenActiveNotificationDoesNotExist() throws Exception {
		given(deleteNotificationUseCase.deleteNotification(1L, "user1")).willReturn(false);

		mockMvc.perform(delete("/api/sse/1"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type")
						.value("https://api.pikume.com/problems/common/resource-not-found"))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("알림을 찾을 수 없습니다."))
				.andExpect(jsonPath("$.instance").value("/api/sse/1"));
	}

	@Test
	@DisplayName("MVC에서 Notification Page의 content와 Page 메타데이터 JSON 계약을 유지한다")
	void mvcPreservesNotificationPageJsonContract() throws Exception {
		NotificationResult item = new NotificationResult(
				1L,
				"님이 일기에 댓글을 달았습니다.",
				null,
				null,
				NotificationKind.COMMENT,
				null,
				null,
				false,
				null,
				null,
				null);
		given(queryNotificationPageUseCase.queryNotifications(eq("user1"), any()))
				.willReturn(new PageResult<>(List.of(item), 1, 2, 5));

		mockMvc.perform(get("/api/sse/notifications")
						.param("page", "1")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content[0].id").value(1))
				.andExpect(jsonPath("$.content[0].type").value("COMMENT"))
				.andExpect(jsonPath("$.content[0].nickname").isEmpty())
				.andExpect(jsonPath("$.number").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.totalPages").value(3))
				.andExpect(jsonPath("$.numberOfElements").value(1))
				.andExpect(jsonPath("$.first").value(false))
				.andExpect(jsonPath("$.last").value(false))
				.andExpect(jsonPath("$.empty").value(false))
				.andExpect(jsonPath("$.sort.sorted").value(true))
				.andExpect(jsonPath("$.pageable.pageNumber").value(1))
				.andExpect(jsonPath("$.pageable.pageSize").value(2))
				.andExpect(jsonPath("$.pageable.offset").value(2));
	}

	private HandlerMethodArgumentResolver principalResolver() {
		return new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
						&& parameter.getParameterType().equals(UserPrincipal.class);
			}

			@Override
			public Object resolveArgument(
					@NotNull MethodParameter parameter,
					ModelAndViewContainer mavContainer,
					@NotNull NativeWebRequest webRequest,
					WebDataBinderFactory binderFactory) {
				return new UserPrincipal("user1", "pikume");
			}
		};
	}
}
