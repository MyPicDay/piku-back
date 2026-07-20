package com.pikume.back.notification.application.service;

import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.policy.NotificationPresentationPolicy;
import com.pikume.back.notification.application.port.out.LoadNotificationDiaryContextsPort;
import com.pikume.back.notification.application.port.out.LoadNotificationSendersPort;
import com.pikume.back.notification.application.readmodel.NotificationDiaryContextView;
import com.pikume.back.notification.application.readmodel.NotificationListItemView;
import com.pikume.back.notification.application.readmodel.NotificationSenderView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationListAssembler")
class NotificationListAssemblerTest {

	@InjectMocks
	private NotificationListAssembler assembler;

	@Mock
	private LoadNotificationSendersPort loadNotificationSendersPort;
	@Mock
	private LoadNotificationDiaryContextsPort loadNotificationDiaryContextsPort;
	@Spy
	private NotificationPresentationPolicy presentationPolicy;

	@Nested
	@DisplayName("assemble - 목록 조합")
	class Assemble {

		@Test
		@DisplayName("누락 Diary 알림을 제외하고 현재 Page total 계산을 유지한다")
		void excludesMissingDiaryAndAdjustsCurrentPageTotal() {
			Notification activeDiary = notification(1L, "active-sender", NotificationType.COMMENT, 10L);
			Notification missingDiary = notification(2L, "missing-sender", NotificationType.COMMENT, 20L);
			Notification friendRequest = notification(3L, "friend-sender", NotificationType.FRIEND_REQUEST, null);
			given(loadNotificationDiaryContextsPort.loadNotificationDiaryContexts(Set.of(10L, 20L)))
					.willReturn(Map.of(
							10L,
							new NotificationDiaryContextView(10L, "thumbnail-url", null, "owner-id", false)));
			given(loadNotificationSendersPort.loadNotificationSenders(Set.of("active-sender", "friend-sender")))
					.willReturn(Map.of(
							"active-sender",
							new NotificationSenderView("active-sender", "활성 발신자", "active-avatar"),
							"friend-sender",
							new NotificationSenderView("friend-sender", "친구 발신자", "friend-avatar")));

			PageResult<NotificationListItemView> result = assembler.assemble(
					new PageResult<>(List.of(activeDiary, missingDiary, friendRequest), 0, 10, 3));

			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getTotalElements()).isEqualTo(2);
			assertThat(result.getContent()).extracting(NotificationListItemView::id)
					.containsExactly(1L, 3L);
			assertThat(result.getContent().get(0).kind()).isEqualTo(NotificationKind.COMMENT);
			assertThat(result.getContent().get(0).nickname()).isEqualTo("활성 발신자");
			assertThat(result.getContent().get(0).thumbnailUrl()).isEqualTo("thumbnail-url");
			then(loadNotificationSendersPort).should()
					.loadNotificationSenders(Set.of("active-sender", "friend-sender"));
		}

		@Test
		@DisplayName("익명 Diary 알림은 발신자 조회와 식별값을 마스킹한다")
		void masksAnonymousDiarySender() {
			Notification anonymous = notification(1L, "sender-id", NotificationType.LIKE, 10L);
			given(loadNotificationDiaryContextsPort.loadNotificationDiaryContexts(Set.of(10L)))
					.willReturn(Map.of(
							10L,
							new NotificationDiaryContextView(10L, null, null, "owner-id", true)));

			PageResult<NotificationListItemView> result = assembler.assemble(
					new PageResult<>(List.of(anonymous), 0, 10, 1));

			NotificationListItemView item = result.getContent().get(0);
			assertThat(item.nickname()).isEqualTo("익명");
			assertThat(item.avatarUrl()).isNull();
			assertThat(item.diaryUserId()).isNull();
			then(loadNotificationSendersPort).shouldHaveNoInteractions();
		}
	}

	private Notification notification(Long id, String senderId, NotificationType type, Long diaryId) {
		return new Notification(id, "receiver-id", senderId, type, diaryId, false, null);
	}
}
