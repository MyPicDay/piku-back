package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.MarkAllNotificationsReadPort;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationReadService")
class NotificationReadServiceTest {

	@InjectMocks
	private NotificationReadService service;

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private MarkAllNotificationsReadPort markAllNotificationsReadPort;

	@Nested
	@DisplayName("markNotificationRead - 단건 읽음")
	class MarkNotificationRead {

		@Test
		@DisplayName("수신자가 자신의 알림을 읽음 처리한다")
		void marksOwnedNotificationRead() {
			Notification notification = notification("receiver-id");
			given(loadNotificationPort.loadActiveNotification(1L)).willReturn(Optional.of(notification));

			boolean result = service.markNotificationRead(1L, "receiver-id");

			assertThat(result).isTrue();
			assertThat(notification.getIsRead()).isTrue();
		}

		@Test
		@DisplayName("다른 사용자의 알림은 읽음 처리하지 않는다")
		void rejectsAnotherUsersNotification() {
			Notification notification = notification("receiver-id");
			given(loadNotificationPort.loadActiveNotification(1L)).willReturn(Optional.of(notification));

			boolean result = service.markNotificationRead(1L, "other-id");

			assertThat(result).isFalse();
			assertThat(notification.getIsRead()).isFalse();
		}

		@Test
		@DisplayName("활성 알림이 없으면 읽음 처리하지 않는다")
		void rejectsMissingActiveNotification() {
			given(loadNotificationPort.loadActiveNotification(1L)).willReturn(Optional.empty());

			boolean result = service.markNotificationRead(1L, "receiver-id");

			assertThat(result).isFalse();
		}
	}

	@Test
	@DisplayName("사용자의 모든 알림 읽음을 목적별 Port에 위임한다")
	void marksAllNotificationsRead() {
		service.markAllNotificationsRead("receiver-id");

		then(markAllNotificationsReadPort).should().markAllNotificationsRead("receiver-id");
	}

	private Notification notification(String receiverId) {
		return new Notification(receiverId, "sender-id", NotificationType.COMMENT, 10L);
	}
}
