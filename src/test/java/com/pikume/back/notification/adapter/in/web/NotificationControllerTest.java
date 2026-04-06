package com.pikume.back.notification.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.notification.application.port.in.NotificationUseCase;
import com.pikume.back.notification.application.port.in.SseUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

	@Mock
	private NotificationUseCase notificationUseCase;

	@Mock
	private SseUseCase sseUseCase;

	@Mock
	private RequestMetaMapper requestMetaMapper;

	private NotificationController notificationController;

	@BeforeEach
	void setUp() {
		notificationController = new NotificationController(
				notificationUseCase,
				sseUseCase,
				requestMetaMapper,
				new ProblemDetailFactory());
	}

	@Test
	@DisplayName("PATCH /api/sse/{notificationId}는 알림이 없으면 Problem Details를 반환한다")
	void markAsReadReturnsProblemDetailWhenNotificationDoesNotExist() throws Exception {
		given(notificationUseCase.markAsRead(1L, "user1")).willReturn(false);

		ResponseEntity<?> response = notificationController.markAsRead(
				1L,
				new CustomUserDetails("user1", "user@example.com", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/sse/1");
	}

	@Test
	@DisplayName("DELETE /api/sse/{notificationId}는 알림이 없으면 Problem Details를 반환한다")
	void deleteNotificationReturnsProblemDetailWhenNotificationDoesNotExist() throws Exception {
		given(notificationUseCase.deleteNotification(1L, "user1")).willReturn(false);

		ResponseEntity<?> response = notificationController.deleteNotification(
				1L,
				new CustomUserDetails("user1", "user@example.com", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/sse/1");
	}
}
