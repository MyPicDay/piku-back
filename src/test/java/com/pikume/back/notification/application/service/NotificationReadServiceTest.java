package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.port.out.MarkAllNotificationsReadPort;
import com.pikume.back.notification.application.port.out.MarkNotificationReadPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationReadService")
class NotificationReadServiceTest {

	@InjectMocks
	private NotificationReadService service;

	@Mock
	private MarkNotificationReadPort markNotificationReadPort;
	@Mock
	private MarkAllNotificationsReadPort markAllNotificationsReadPort;

	@Test
	@DisplayName("활성 알림 읽음 처리를 목적별 Port에 위임한다")
	void marksNotificationRead() {
		service.markNotificationRead(1L, "receiver-id");

		then(markNotificationReadPort).should().markNotificationReadIfActive(1L, "receiver-id");
	}

	@Test
	@DisplayName("사용자의 모든 알림 읽음을 목적별 Port에 위임한다")
	void marksAllNotificationsRead() {
		service.markAllNotificationsRead("receiver-id");

		then(markAllNotificationsReadPort).should().markAllNotificationsRead("receiver-id");
	}
}
