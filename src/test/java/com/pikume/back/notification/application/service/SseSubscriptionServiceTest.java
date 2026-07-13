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
import com.pikume.back.notification.application.exception.NotificationStreamSendException;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;
import com.pikume.back.notification.application.port.out.NotificationStreamPort;

import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
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

		@Test
		@DisplayName("초기 이벤트 전송 실패는 연결을 정리하고 예외로 올리지 않는다")
		void subscribeDeletesEmitterWithoutThrowingWhenInitialSendFails() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(3L);
			willThrow(new NotificationStreamSendException("SSE 전송 실패", new IOException("Broken pipe")))
					.given(connection).send(any(NotificationStreamMessage.class));

			assertThatCode(() -> sseSubscriptionService.subscribe("user-id", connection))
					.doesNotThrowAnyException();

			ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
			then(notificationStreamPort).should().save(emitterIdCaptor.capture(), eq("user-id"), eq(connection));
			then(notificationStreamPort).should().delete("user-id", emitterIdCaptor.getValue());
			then(loadNotificationPort).should(never()).existsFriendRequestByReceiverId("user-id");
		}

		@Test
		@DisplayName("예상하지 못한 초기 전송 예외는 숨기지 않는다")
		void subscribePropagatesUnexpectedInitialSendFailure() {
			RuntimeException unexpectedFailure = new IllegalStateException("serialization bug");
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(3L);
			willThrow(unexpectedFailure).given(connection).send(any(NotificationStreamMessage.class));

			assertThatThrownBy(() -> sseSubscriptionService.subscribe("user-id", connection))
					.isSameAs(unexpectedFailure);

			then(notificationStreamPort).should().save(anyString(), eq("user-id"), eq(connection));
			then(notificationStreamPort).should().delete(eq("user-id"), anyString());
			then(loadNotificationPort).should(never()).existsFriendRequestByReceiverId("user-id");
		}

		@Test
		@DisplayName("SSE 연결 완료 콜백이 발생하면 사용자의 emitter를 삭제한다")
		void subscribeDeletesEmitterWhenConnectionCompletionCallbackRuns() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(0L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			sseSubscriptionService.subscribe("user-id", connection);

			ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Runnable> completionHandlerCaptor = ArgumentCaptor.forClass(Runnable.class);
			then(notificationStreamPort).should().save(emitterIdCaptor.capture(), eq("user-id"), eq(connection));
			then(connection).should().onCompletion(completionHandlerCaptor.capture());

			completionHandlerCaptor.getValue().run();

			then(notificationStreamPort).should().delete("user-id", emitterIdCaptor.getValue());
		}

		@Test
		@DisplayName("SSE 연결 오류 콜백이 발생하면 emitter를 삭제한다")
		@SuppressWarnings("unchecked")
		void subscribeDeletesEmitterWhenConnectionErrorCallbackRuns() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(0L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			sseSubscriptionService.subscribe("user-id", connection);

			ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Consumer<Throwable>> errorHandlerCaptor = ArgumentCaptor.forClass(Consumer.class);
			then(notificationStreamPort).should().save(emitterIdCaptor.capture(), eq("user-id"), eq(connection));
			then(connection).should().onError(errorHandlerCaptor.capture());

			errorHandlerCaptor.getValue().accept(
					new IOException("현재 연결은 사용자의 호스트 시스템의 소프트웨어에 의해 중단되었습니다"));

			then(notificationStreamPort).should().delete("user-id", emitterIdCaptor.getValue());
		}

		@Test
		@DisplayName("SSE 연결 타임아웃 콜백이 발생하면 사용자의 emitter를 삭제하고 연결을 완료한다")
		void subscribeDeletesEmitterAndCompletesConnectionWhenTimeoutCallbackRuns() {
			given(loadNotificationPort.countUnreadByReceiverId("user-id")).willReturn(0L);
			given(loadNotificationPort.existsFriendRequestByReceiverId("user-id")).willReturn(false);

			sseSubscriptionService.subscribe("user-id", connection);

			ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Runnable> timeoutHandlerCaptor = ArgumentCaptor.forClass(Runnable.class);
			then(notificationStreamPort).should().save(emitterIdCaptor.capture(), eq("user-id"), eq(connection));
			then(connection).should().onTimeout(timeoutHandlerCaptor.capture());

			timeoutHandlerCaptor.getValue().run();

			then(notificationStreamPort).should().delete("user-id", emitterIdCaptor.getValue());
			then(connection).should().complete();
		}
	}
}
