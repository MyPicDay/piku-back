package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.port.out.DeleteNotificationsByDiaryPort;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeletionService")
class NotificationDeletionServiceTest {

	@InjectMocks
	private NotificationDeletionService service;

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private DeleteNotificationsByDiaryPort deleteNotificationsByDiaryPort;

	@Nested
	@DisplayName("deleteNotification - 단건 삭제")
	class DeleteNotification {

		@Test
		@DisplayName("수신자가 자신의 알림을 삭제한다")
		void deletesOwnedNotification() {
			Notification notification = notification("receiver-id");
			given(loadNotificationPort.loadNotification(1L)).willReturn(notification);

			boolean result = service.deleteNotification(1L, "receiver-id");

			assertThat(result).isTrue();
			assertThat(notification.isDeleted()).isTrue();
		}

		@Test
		@DisplayName("다른 사용자의 알림은 삭제하지 않는다")
		void rejectsAnotherUsersNotification() {
			Notification notification = notification("receiver-id");
			given(loadNotificationPort.loadNotification(1L)).willReturn(notification);

			boolean result = service.deleteNotification(1L, "other-id");

			assertThat(result).isFalse();
			assertThat(notification.isDeleted()).isFalse();
		}
	}

	@Test
	@DisplayName("Diary 식별자가 없으면 연관 알림 삭제를 요청하지 않는다")
	void skipsDeletionWithoutDiaryId() {
		service.deleteNotificationsByDiary(null);

		then(deleteNotificationsByDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("Diary 연관 알림 삭제를 목적별 Port에 위임한다")
	void deletesNotificationsByDiary() {
		service.deleteNotificationsByDiary(10L);

		then(deleteNotificationsByDiaryPort).should().deleteNotificationsByDiary(10L);
	}

	private Notification notification(String receiverId) {
		return new Notification(receiverId, "sender-id", NotificationType.COMMENT, 10L);
	}
}
