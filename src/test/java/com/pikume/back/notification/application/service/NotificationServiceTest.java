package com.pikume.back.notification.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.port.out.LoadDiaryForNotificationPort;
import com.pikume.back.notification.application.port.out.LoadNotificationListViewPort;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.LoadUserForNotificationPort;
import com.pikume.back.notification.application.port.out.NotificationStreamPort;
import com.pikume.back.notification.application.port.out.PushNotificationPort;
import com.pikume.back.notification.application.port.out.SaveNotificationPort;
import com.pikume.back.notification.application.readmodel.NotificationListView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
	private LoadNotificationListViewPort loadNotificationListViewPort;
	@Mock
	private PushNotificationPort pushNotificationPort;
	@Mock
	private NotificationStreamPort notificationStreamPort;
	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/sse", "TestAgent", "127.0.0.1");

	@Nested
	@DisplayName("sendNotification - 알림 발송")
	class SendNotification {

		@Test
		@DisplayName("알림을 저장하고 트랜잭션 커밋 후 SSE 및 FCM을 통해 전송한다")
		void savesAndSendsNotification() throws Exception {
			given(saveNotificationPort.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("avatar-url");
			given(loadDiaryForNotificationPort.getDiaryThumbnailUrl(1L)).willReturn("thumb.jpg");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of("fcm-token"));

			TransactionSynchronizationManager.initSynchronization();
			try {
				notificationService.sendNotification("receiver-id", NotificationType.COMMENT, "sender-id", 1L, null);

				ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
				then(saveNotificationPort).should().save(notificationCaptor.capture());
				then(notificationStreamPort).should(never()).sendToUser(any(), any());
				then(pushNotificationPort).should(never()).sendMessage(any(), any());

				Notification saved = notificationCaptor.getValue();
				assertThat(saved.getReceiverId()).isEqualTo("receiver-id");
				assertThat(saved.getSenderId()).isEqualTo("sender-id");
				assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT);
				assertThat(saved.getDiaryId()).isEqualTo(1L);
				assertThat(saved.getIsRead()).isFalse();

				TransactionSynchronization afterCommitSynchronization =
						TransactionSynchronizationManager.getSynchronizations().get(0);
				afterCommitSynchronization.afterCommit();

				ArgumentCaptor<NotificationStreamMessage> messageCaptor = ArgumentCaptor.forClass(NotificationStreamMessage.class);
				then(notificationStreamPort).should().sendToUser(eq("receiver-id"), messageCaptor.capture());
				then(pushNotificationPort).should().sendMessage(eq("fcm-token"), contains("보낸이"));
				assertThat(messageCaptor.getValue().data()).isNotNull();
			} finally {
				TransactionSynchronizationManager.clearSynchronization();
			}
		}

		@Test
		@DisplayName("diaryId가 null이면 썸네일을 조회하지 않는다")
		void skipsThumbnailWhenNoDiary() {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("avatar-url");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of());

			notificationService.sendNotification("receiver-id", NotificationType.FRIEND_REQUEST, "sender-id", null, null);

			then(loadDiaryForNotificationPort).should(never()).getDiaryThumbnailUrl(any());
		}

		@Test
		@DisplayName("FCM 토큰이 없으면 푸시를 전송하지 않는다")
		void skipsPushWhenNoTokens() throws Exception {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("url");
			given(loadDiaryForNotificationPort.getDiaryThumbnailUrl(1L)).willReturn("thumb.jpg");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of());

			notificationService.sendNotification("receiver-id", NotificationType.LIKE, "sender-id", 1L, null);

			then(pushNotificationPort).should(never()).sendMessage(any(), any());
		}

		@Test
		@DisplayName("FCM 전송 실패 시 해당 토큰을 삭제한다")
		void deletesTokenOnFcmFailure() throws Exception {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("url");
			given(loadDiaryForNotificationPort.getDiaryThumbnailUrl(1L)).willReturn("thumb.jpg");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of("bad-token"));
			doThrow(new RuntimeException("FCM fail")).when(pushNotificationPort).sendMessage(eq("bad-token"), any());

			notificationService.sendNotification("receiver-id", NotificationType.COMMENT, "sender-id", 1L, null);

			then(pushNotificationPort).should().deleteToken("bad-token");
		}

		@Test
		@DisplayName("커밋 후 SSE 전송 실패가 FCM 전송을 막지 않는다")
		void sendsPushEvenWhenStreamFailsAfterCommit() throws Exception {
			given(saveNotificationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
			given(loadUserForNotificationPort.getUserNickname("sender-id")).willReturn("보낸이");
			given(loadUserForNotificationPort.getUserAvatar("sender-id")).willReturn("avatar.jpg");
			given(loadUserForNotificationPort.getUserAvatarUrl("avatar.jpg", null)).willReturn("url");
			given(pushNotificationPort.getTokenByUserId("receiver-id")).willReturn(Set.of("fcm-token"));
			doThrow(new IllegalStateException("SSE fail"))
					.when(notificationStreamPort).sendToUser(eq("receiver-id"), any());

			TransactionSynchronizationManager.initSynchronization();
			try {
				notificationService.sendNotification("receiver-id", NotificationType.FRIEND_REQUEST, "sender-id", null, null);

				TransactionSynchronization afterCommitSynchronization =
						TransactionSynchronizationManager.getSynchronizations().get(0);
				assertThatCode(afterCommitSynchronization::afterCommit)
						.doesNotThrowAnyException();

				then(notificationStreamPort).should().sendToUser(eq("receiver-id"), any());
				then(pushNotificationPort).should().sendMessage(eq("fcm-token"), contains("보낸이"));
			} finally {
				TransactionSynchronizationManager.clearSynchronization();
			}
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
			PageQuery pageQuery = PageQuery.of(0, 10);
			NotificationListView notification = new NotificationListView(
					1L,
					"보낸이",
					"avatar.jpg",
					NotificationType.COMMENT,
					1L,
					"thumb.jpg",
					false,
					LocalDateTime.of(2026, 3, 8, 12, 0),
					null,
					null);
			PageResult<NotificationListView> page = new PageResult<>(List.of(notification), 0, 10, 1);

			given(loadNotificationListViewPort.loadNotifications("receiver-id", pageQuery)).willReturn(page);
			given(imagePathToUrlConverter.userAvatarImageUrl("avatar.jpg", requestMetaInfo)).willReturn("avatar-url");

			PageResult<NotificationResult> result = notificationService.getNotifications("receiver-id", requestMetaInfo, pageQuery);

			assertThat(result.getContent()).hasSize(1);
			NotificationResult dto = result.getContent().get(0);
			assertThat(dto.nickname()).isEqualTo("보낸이");
			assertThat(dto.avatarUrl()).isEqualTo("avatar-url");
			assertThat(dto.type()).isEqualTo(NotificationType.COMMENT);
			assertThat(dto.thumbnailUrl()).isEqualTo("thumb.jpg");
		}

		@Test
		@DisplayName("알림이 없으면 빈 페이지를 반환한다")
		void returnsEmptyPage() {
			PageQuery pageQuery = PageQuery.of(0, 10);
			given(loadNotificationListViewPort.loadNotifications("receiver-id", pageQuery)).willReturn(PageResult.empty(pageQuery));

			PageResult<NotificationResult> result = notificationService.getNotifications("receiver-id", requestMetaInfo, pageQuery);

			assertThat(result.getContent()).isEmpty();
		}

		@Test
		@DisplayName("diaryId가 null인 알림은 썸네일 없이 반환한다")
		void returnsNullThumbnailWhenNoDiary() {
			PageQuery pageQuery = PageQuery.of(0, 10);
			NotificationListView notification = new NotificationListView(
					1L,
					"보낸이",
					"avatar.jpg",
					NotificationType.FRIEND_ACCEPT,
					null,
					null,
					false,
					LocalDateTime.of(2026, 3, 8, 12, 10),
					null,
					null);
			PageResult<NotificationListView> page = new PageResult<>(List.of(notification), 0, 10, 1);

			given(loadNotificationListViewPort.loadNotifications("receiver-id", pageQuery)).willReturn(page);
			given(imagePathToUrlConverter.userAvatarImageUrl("avatar.jpg", requestMetaInfo)).willReturn("url");

			PageResult<NotificationResult> result = notificationService.getNotifications("receiver-id", requestMetaInfo, pageQuery);

			assertThat(result.getContent().get(0).thumbnailUrl()).isNull();
			then(loadNotificationListViewPort).should().loadNotifications("receiver-id", pageQuery);
			then(loadNotificationPort).shouldHaveNoInteractions();
			then(loadUserForNotificationPort).shouldHaveNoInteractions();
			then(loadDiaryForNotificationPort).shouldHaveNoInteractions();
		}
	}
}
