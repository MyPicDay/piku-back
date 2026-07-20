package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.NotificationSsePayload;
import com.pikume.back.notification.application.dto.RecordNotificationCommand;
import com.pikume.back.notification.application.exception.NotificationSenderNotFoundException;
import com.pikume.back.notification.application.policy.NotificationPresentationPolicy;
import com.pikume.back.notification.application.port.out.LoadNotificationDiaryContextsPort;
import com.pikume.back.notification.application.port.out.LoadNotificationSendersPort;
import com.pikume.back.notification.application.port.out.RecordNotificationPort;
import com.pikume.back.notification.application.port.out.ScheduleNotificationDeliveryPort;
import com.pikume.back.notification.application.readmodel.NotificationDiaryContextView;
import com.pikume.back.notification.application.readmodel.NotificationSenderView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRecordingService")
class NotificationRecordingServiceTest {

	@InjectMocks
	private NotificationRecordingService service;

	@Mock
	private RecordNotificationPort recordNotificationPort;
	@Mock
	private LoadNotificationSendersPort loadNotificationSendersPort;
	@Mock
	private LoadNotificationDiaryContextsPort loadNotificationDiaryContextsPort;
	@Mock
	private ScheduleNotificationDeliveryPort scheduleNotificationDeliveryPort;
	@Spy
	private NotificationPresentationPolicy presentationPolicy;

	@Nested
	@DisplayName("recordNotification - 알림 기록")
	class RecordNotification {

		@Test
		@DisplayName("알림을 기록하고 Notification 소유 전달 요청을 예약한다")
		void recordsAndSchedulesDelivery() {
			given(loadNotificationDiaryContextsPort.loadNotificationDiaryContexts(Set.of(10L)))
					.willReturn(Map.of(
							10L,
							new NotificationDiaryContextView(10L, "thumbnail-url", null, "owner-id", false)));
			given(loadNotificationSendersPort.loadNotificationSenders(Set.of("sender-id")))
					.willReturn(Map.of(
							"sender-id",
							new NotificationSenderView("sender-id", "보낸이", "avatar-url")));
			given(recordNotificationPort.recordNotification(any(Notification.class)))
					.willReturn(new Notification(
							99L,
							"receiver-id",
							"sender-id",
							NotificationType.COMMENT,
							10L,
							false,
							null));

			service.recordNotification(new RecordNotificationCommand(
					"receiver-id",
					NotificationKind.COMMENT,
					"sender-id",
					10L));

			ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
			then(recordNotificationPort).should().recordNotification(notificationCaptor.capture());
			assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.COMMENT);
			assertThat(notificationCaptor.getValue().getReceiverId()).isEqualTo("receiver-id");

			ArgumentCaptor<NotificationDeliveryRequest> deliveryCaptor =
					ArgumentCaptor.forClass(NotificationDeliveryRequest.class);
			then(scheduleNotificationDeliveryPort).should().scheduleNotificationDelivery(deliveryCaptor.capture());
			NotificationDeliveryRequest delivery = deliveryCaptor.getValue();
			assertThat(delivery.notificationId()).isEqualTo(99L);
			assertThat(delivery.receiverId()).isEqualTo("receiver-id");
			assertThat(delivery.pushBody()).isEqualTo("보낸이님이 일기에 댓글을 달았습니다.");
			assertThat(delivery.streamMessage().data()).isInstanceOf(NotificationSsePayload.class);
			NotificationSsePayload payload = (NotificationSsePayload) delivery.streamMessage().data();
			assertThat(payload.type()).isEqualTo(NotificationKind.COMMENT);
			assertThat(payload.senderId()).isEqualTo("sender-id");
			assertThat(payload.senderNickname()).isEqualTo("보낸이");
			assertThat(payload.senderAvatarUrl()).isEqualTo("avatar-url");
			assertThat(payload.thumbnailUrl()).isEqualTo("thumbnail-url");
		}

		@Test
		@DisplayName("연관 Diary가 없으면 알림을 기록하거나 예약하지 않는다")
		void skipsMissingDiary() {
			given(loadNotificationDiaryContextsPort.loadNotificationDiaryContexts(Set.of(10L)))
					.willReturn(Map.of());

			service.recordNotification(new RecordNotificationCommand(
					"receiver-id",
					NotificationKind.COMMENT,
					"sender-id",
					10L));

			then(recordNotificationPort).shouldHaveNoInteractions();
			then(scheduleNotificationDeliveryPort).shouldHaveNoInteractions();
			then(loadNotificationSendersPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("익명 Diary 알림은 발신자를 조회하지 않고 표시 정보를 마스킹한다")
		void masksAnonymousDiarySender() {
			given(loadNotificationDiaryContextsPort.loadNotificationDiaryContexts(Set.of(10L)))
					.willReturn(Map.of(
							10L,
							new NotificationDiaryContextView(10L, "thumbnail-url", null, "owner-id", true)));
			given(recordNotificationPort.recordNotification(any(Notification.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			service.recordNotification(new RecordNotificationCommand(
					"receiver-id",
					NotificationKind.LIKE,
					"sender-id",
					10L));

			then(loadNotificationSendersPort).shouldHaveNoInteractions();
			ArgumentCaptor<NotificationDeliveryRequest> deliveryCaptor =
					ArgumentCaptor.forClass(NotificationDeliveryRequest.class);
			then(scheduleNotificationDeliveryPort).should().scheduleNotificationDelivery(deliveryCaptor.capture());
			NotificationSsePayload payload =
					(NotificationSsePayload) deliveryCaptor.getValue().streamMessage().data();
			assertThat(payload.senderId()).isNull();
			assertThat(payload.senderNickname()).isEqualTo("익명");
			assertThat(payload.senderAvatarUrl()).isNull();
			assertThat(deliveryCaptor.getValue().pushBody()).isEqualTo("익명님이 회원님의 일기를 좋아합니다.");
		}

		@Test
		@DisplayName("비익명 발신자 메타데이터가 없으면 null 문구를 전달하지 않고 기록을 실패시킨다")
		void rejectsMissingNamedSenderMetadata() {
			given(loadNotificationSendersPort.loadNotificationSenders(Set.of("missing-sender")))
					.willReturn(Map.of());
			given(recordNotificationPort.recordNotification(any(Notification.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			assertThatThrownBy(() -> service.recordNotification(new RecordNotificationCommand(
					"receiver-id",
					NotificationKind.FRIEND_REQUEST,
					"missing-sender",
					null)))
					.isInstanceOf(NotificationSenderNotFoundException.class)
					.hasMessageContaining("missing-sender");

			then(scheduleNotificationDeliveryPort).shouldHaveNoInteractions();
		}
	}
}
