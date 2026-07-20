package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.notification.application.port.in.SubscribeNotificationStreamUseCase;
import com.pikume.back.notification.application.stream.NotificationStreamConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("NotificationStreamController")
class NotificationStreamControllerTest {

	private final SubscribeNotificationStreamUseCase subscribeNotificationStreamUseCase =
			mock(SubscribeNotificationStreamUseCase.class);
	private final NotificationStreamController controller =
			new NotificationStreamController(subscribeNotificationStreamUseCase);

	@Test
	@DisplayName("GET /api/sse/subscribe는 연결을 생성하고 구독 Use Case에 전달한다")
	void subscribesNotificationStream() {
		SseEmitter result = controller.subscribe(new CustomUserDetails("user-id", "pikume"));

		assertThat(result).isNotNull();
		then(subscribeNotificationStreamUseCase).should()
				.subscribeToNotifications(
						org.mockito.ArgumentMatchers.eq("user-id"),
						any(NotificationStreamConnection.class));
	}
}
