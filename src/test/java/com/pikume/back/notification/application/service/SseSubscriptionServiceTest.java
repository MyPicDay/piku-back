package com.pikume.back.notification.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.SseEmitterPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseSubscriptionService - SSE 구독 관리")
class SseSubscriptionServiceTest {

	@InjectMocks
	private SseSubscriptionService sseSubscriptionService;

	@Mock
	private SseEmitterPort sseEmitterPort;
	@Mock
	private LoadNotificationPort loadNotificationPort;

	@Nested
	@DisplayName("subscribe - SSE 구독")
	class Subscribe {

		@Test
		@DisplayName("SSE 구독 시 emitter를 생성하고 미읽음 개수를 전송한다")
		void subscribeSendsUnreadCount() {
			SseEmitter emitter = new SseEmitter();
			given(sseEmitterPort.save(anyString(), any(SseEmitter.class))).willReturn(emitter);
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(3L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			SseEmitter result = sseSubscriptionService.subscribe("user-id");

			assertThat(result).isNotNull();
			then(sseEmitterPort).should().save(anyString(), any(SseEmitter.class));
			then(loadNotificationPort).should().countUnreadByReceiverId("user-id");
		}

		@Test
		@DisplayName("구독 시 친구 요청이 있으면 FriendRequest 이벤트를 전송한다")
		void subscribeSendsFriendRequestEvent() {
			SseEmitter emitter = new SseEmitter();
			given(sseEmitterPort.save(anyString(), any(SseEmitter.class))).willReturn(emitter);
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(1L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(true);

			SseEmitter result = sseSubscriptionService.subscribe("user-id");

			assertThat(result).isNotNull();
			then(loadNotificationPort).should().existsFriendRequestByReceiverId("user-id");
		}

		@Test
		@DisplayName("미읽음 알림이 없으면 0을 전송한다")
		void subscribeSendsZeroWhenNoUnread() {
			SseEmitter emitter = new SseEmitter();
			given(sseEmitterPort.save(anyString(), any(SseEmitter.class))).willReturn(emitter);
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(0L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			SseEmitter result = sseSubscriptionService.subscribe("user-id");

			assertThat(result).isNotNull();
		}
	}
}
