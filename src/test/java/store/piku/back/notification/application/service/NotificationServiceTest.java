package store.piku.back.notification.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.notification.adapter.in.web.dto.NotificationResponseDTO;
import store.piku.back.notification.application.port.out.*;
import store.piku.back.notification.domain.Notification;
import store.piku.back.notification.domain.exception.NotificationNotFoundException;
import store.piku.back.notification.domain.vo.NotificationType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - 알림 CRUD 및 발송")
class NotificationServiceTest {

	@InjectMocks
	private NotificationService notificationService;

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private SaveNotificationPort saveNotificationPort;
	@Mock
	private LoadUserForNotificationPort loadUserForNotificationPort;
	@Mock
	private LoadDiaryForNotificationPort loadDiaryForNotificationPort;
	@Mock
	private PushNotificationPort pushNotificationPort;
	@Mock
	private SseEmitterPort sseEmitterPort;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/sse", "TestAgent", "127.0.0.1");

	@Nested
	@DisplayName("sendNotification - 알림 발송")
	class SendNotification {

		@Test
		@DisplayName("알림을 저장하고 SSE 및 FCM을 통해 전송한다")
		void savesAndSendsNotification() throws Exception {
			given(saveNotificationPort.save(any(Notification.class)))
					.willAnswer(inv -> inv.getArgument(0));
			given(sseEmitterPort.findAllByUserId("receiver-id"))
					.willReturn(Collections.emptyMap());
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("avatar-url");
			given(loadDiaryForNotificationPort.getDiaryThumbnailUrl(1L)).willReturn("thumb.jpg");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of("fcm-token"));

			notificationService.sendNotification(
					"receiver-id", NotificationType.COMMENT, "sender-id", 1L, null);

			ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
			then(saveNotificationPort).should().save(captor.capture());
			Notification saved = captor.getValue();
			assertThat(saved.getReceiverId()).isEqualTo("receiver-id");
			assertThat(saved.getSenderId()).isEqualTo("sender-id");
			assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT);
			assertThat(saved.getDiaryId()).isEqualTo(1L);
			assertThat(saved.getIsRead()).isFalse();

			then(pushNotificationPort).should().sendMessage(eq("fcm-token"), contains("보낸이"));
		}

		@Test
		@DisplayName("diaryId가 null이면 썸네일을 조회하지 않는다")
		void skipsThumbnailWhenNoDiary() {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(sseEmitterPort.findAllByUserId("receiver-id")).willReturn(Collections.emptyMap());
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("avatar-url");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Collections.emptySet());

			notificationService.sendNotification(
					"receiver-id", NotificationType.FRIEND_REQUEST, "sender-id", null, null);

			then(loadDiaryForNotificationPort).should(never()).getDiaryThumbnailUrl(any());
		}

		@Test
		@DisplayName("FCM 토큰이 없으면 푸시를 전송하지 않는다")
		void skipsPushWhenNoTokens() throws Exception {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(sseEmitterPort.findAllByUserId("receiver-id")).willReturn(Collections.emptyMap());
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("url");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Collections.emptySet());

			notificationService.sendNotification(
					"receiver-id", NotificationType.LIKE, "sender-id", 1L, null);

			then(pushNotificationPort).should(never()).sendMessage(any(), any());
		}

		@Test
		@DisplayName("FCM 전송 실패 시 해당 토큰을 삭제한다")
		void deletesTokenOnFcmFailure() throws Exception {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(sseEmitterPort.findAllByUserId("receiver-id")).willReturn(Collections.emptyMap());
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("url");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of("bad-token"));
			doThrow(new RuntimeException("FCM fail"))
					.when(pushNotificationPort).sendMessage(eq("bad-token"), any());

			notificationService.sendNotification(
					"receiver-id", NotificationType.COMMENT, "sender-id", 1L, null);

			then(pushNotificationPort).should().deleteToken("bad-token");
		}

		@Test
		@DisplayName("알림 타입별 메세지가 올바르게 생성된다")
		void generatesCorrectMessage() {
			assertThat(notificationService.generateMessage(NotificationType.FRIEND_REQUEST))
					.isEqualTo("님이 친구 요청을 보냈습니다");
			assertThat(notificationService.generateMessage(NotificationType.COMMENT))
					.isEqualTo("님이 일기에 댓글을 달았습니다.");
			assertThat(notificationService.generateMessage(NotificationType.LIKE))
					.isEqualTo("님이 회원님의 일기를 좋아합니다.");
		}
	}

	@Nested
	@DisplayName("getNotifications - 알림 목록 조회")
	class GetNotifications {

		@Test
		@DisplayName("사용자의 알림 목록을 페이징으로 조회한다")
		void returnsPagedNotifications() {
			Pageable pageable = PageRequest.of(0, 10);
			Notification notification = new Notification("receiver-id", "sender-id",
					NotificationType.COMMENT, 1L);
			Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

			given(loadNotificationPort.findAllByReceiverIdAndDeletedAtIsNull("receiver-id", pageable))
					.willReturn(page);
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", requestMetaInfo)).willReturn("avatar-url");
			given(loadDiaryForNotificationPort.getDiaryThumbnailUrl(1L)).willReturn("thumb.jpg");

			Page<NotificationResponseDTO> result = notificationService.getNotifications(
					"receiver-id", requestMetaInfo, pageable);

			assertThat(result.getContent()).hasSize(1);
			NotificationResponseDTO dto = result.getContent().get(0);
			assertThat(dto.getNickname()).isEqualTo("보낸이");
			assertThat(dto.getAvatarUrl()).isEqualTo("avatar-url");
			assertThat(dto.getType()).isEqualTo(NotificationType.COMMENT);
			assertThat(dto.getThumbnailUrl()).isEqualTo("thumb.jpg");
		}

		@Test
		@DisplayName("알림이 없으면 빈 페이지를 반환한다")
		void returnsEmptyPage() {
			Pageable pageable = PageRequest.of(0, 10);
			given(loadNotificationPort.findAllByReceiverIdAndDeletedAtIsNull("receiver-id", pageable))
					.willReturn(Page.empty(pageable));

			Page<NotificationResponseDTO> result = notificationService.getNotifications(
					"receiver-id", requestMetaInfo, pageable);

			assertThat(result.getContent()).isEmpty();
		}

		@Test
		@DisplayName("diaryId가 null인 알림은 썸네일 없이 반환한다")
		void returnsNullThumbnailWhenNoDiary() {
			Pageable pageable = PageRequest.of(0, 10);
			Notification notification = new Notification("receiver-id", "sender-id",
					NotificationType.FRIEND_ACCEPT, null);
			Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

			given(loadNotificationPort.findAllByReceiverIdAndDeletedAtIsNull("receiver-id", pageable))
					.willReturn(page);
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", requestMetaInfo)).willReturn("url");

			Page<NotificationResponseDTO> result = notificationService.getNotifications(
					"receiver-id", requestMetaInfo, pageable);

			assertThat(result.getContent().get(0).getThumbnailUrl()).isNull();
			then(loadDiaryForNotificationPort).should(never()).getDiaryThumbnailUrl(any());
		}
	}

	@Nested
	@DisplayName("markAsRead - 알림 읽음 처리")
	class MarkAsRead {

		@Test
		@DisplayName("본인의 알림을 읽음 처리한다")
		void markAsReadSuccess() {
			Notification notification = new Notification("user-id", "sender-id",
					NotificationType.LIKE, 1L);
			given(loadNotificationPort.findById(1L)).willReturn(notification);

			boolean result = notificationService.markAsRead(1L, "user-id");

			assertThat(result).isTrue();
			assertThat(notification.getIsRead()).isTrue();
		}

		@Test
		@DisplayName("타인의 알림을 읽음 처리하면 false를 반환한다")
		void markAsReadUnauthorized() {
			Notification notification = new Notification("other-user", "sender-id",
					NotificationType.LIKE, 1L);
			given(loadNotificationPort.findById(1L)).willReturn(notification);

			boolean result = notificationService.markAsRead(1L, "user-id");

			assertThat(result).isFalse();
			assertThat(notification.getIsRead()).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다")
		void markAsReadNotFound() {
			given(loadNotificationPort.findById(999L))
					.willThrow(new NotificationNotFoundException("알림이 존재하지 않습니다. ID: 999"));

			assertThatThrownBy(() -> notificationService.markAsRead(999L, "user-id"))
					.isInstanceOf(NotificationNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("markAllAsRead - 모두 읽음 처리")
	class MarkAllAsRead {

		@Test
		@DisplayName("사용자의 모든 알림을 읽음 처리한다")
		void markAllAsReadSuccess() {
			given(loadNotificationPort.markAllAsReadByReceiverId("user-id")).willReturn(5);

			notificationService.markAllAsRead("user-id");

			then(loadNotificationPort).should().markAllAsReadByReceiverId("user-id");
		}
	}

	@Nested
	@DisplayName("deleteNotification - 알림 삭제")
	class DeleteNotification {

		@Test
		@DisplayName("본인의 알림을 삭제한다")
		void deleteSuccess() {
			Notification notification = new Notification("user-id", "sender-id",
					NotificationType.COMMENT, 1L);
			given(loadNotificationPort.findById(1L)).willReturn(notification);

			boolean result = notificationService.deleteNotification(1L, "user-id");

			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("타인의 알림을 삭제하면 false를 반환한다")
		void deleteUnauthorized() {
			Notification notification = new Notification("other-user", "sender-id",
					NotificationType.COMMENT, 1L);
			given(loadNotificationPort.findById(1L)).willReturn(notification);

			boolean result = notificationService.deleteNotification(1L, "user-id");

			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 알림을 삭제하면 예외가 발생한다")
		void deleteNotFound() {
			given(loadNotificationPort.findById(999L))
					.willThrow(new NotificationNotFoundException("알림이 존재하지 않습니다. ID: 999"));

			assertThatThrownBy(() -> notificationService.deleteNotification(999L, "user-id"))
					.isInstanceOf(NotificationNotFoundException.class);
		}
	}
}
