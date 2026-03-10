package com.pikume.back.notification.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;
import com.pikume.back.notification.application.port.out.NotificationStreamPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
@ExtendWith(MockitoExtension.class)
@DisplayName("SseSubscriptionService - SSE 구독 관리")
class SseSubscriptionServiceTest {

	@InjectMocks
	private SseSubscriptionService sseSubscriptionService;

	@Mock
	private NotificationStreamPort notificationStreamPort;
	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private NotificationStreamConnection connection;

	@Nested
	@DisplayName("subscribe - SSE 구독")
	class Subscribe {

		@Test
		@DisplayName("SSE 구독 시 연결을 저장하고 미읽음 개수를 전송한다")
		void subscribeSendsUnreadCount() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(3L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			sseSubscriptionService.subscribe("user-id", connection);

			ArgumentCaptor<NotificationStreamMessage> messageCaptor = ArgumentCaptor.forClass(NotificationStreamMessage.class);
			then(notificationStreamPort).should().save(anyString(), eq("user-id"), eq(connection));
			then(connection).should().onCompletion(any());
			then(connection).should().onTimeout(any());
			then(connection).should().send(messageCaptor.capture());
			assertThat(messageCaptor.getValue().data()).isEqualTo(3L);
			then(loadNotificationPort).should().countUnreadByReceiverId("user-id");
		}

		@Test
		@DisplayName("구독 시 친구 요청이 있으면 FriendRequest 이벤트를 전송한다")
		void subscribeSendsFriendRequestEvent() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(1L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(true);

			sseSubscriptionService.subscribe("user-id", connection);

			ArgumentCaptor<NotificationStreamMessage> messageCaptor = ArgumentCaptor.forClass(NotificationStreamMessage.class);
			then(connection).should(times(2)).send(messageCaptor.capture());
			assertThat(messageCaptor.getAllValues().get(1).eventName()).isEqualTo("FriendRequest");
			assertThat(messageCaptor.getAllValues().get(1).data()).isEqualTo("on");
		}

		@Test
		@DisplayName("미읽음 알림이 없으면 0을 전송한다")
		void subscribeSendsZeroWhenNoUnread() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(0L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			sseSubscriptionService.subscribe("user-id", connection);

			ArgumentCaptor<NotificationStreamMessage> messageCaptor = ArgumentCaptor.forClass(NotificationStreamMessage.class);
			then(connection).should().send(messageCaptor.capture());
			assertThat(messageCaptor.getValue().data()).isEqualTo(0L);
		}
	}
}
