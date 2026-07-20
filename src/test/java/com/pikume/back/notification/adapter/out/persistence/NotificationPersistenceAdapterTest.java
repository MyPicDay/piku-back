package com.pikume.back.notification.adapter.out.persistence;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.readmodel.NotificationSummaryView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPersistenceAdapter")
class NotificationPersistenceAdapterTest {

	@InjectMocks
	private NotificationPersistenceAdapter adapter;

	@Mock
	private NotificationJpaRepository notificationJpaRepository;

	@Test
	@DisplayName("Notification 원본 Page만 조회한다")
	void loadsNotificationPage() {
		Notification notification =
				new Notification("receiver-id", "sender-id", NotificationType.COMMENT, 10L);
		given(notificationJpaRepository.findAllByReceiverIdAndDeletedAtIsNull(
				"receiver-id",
				PageRequest.of(0, 10)))
				.willReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1));

		PageResult<Notification> result =
				adapter.loadNotificationPage("receiver-id", PageQuery.of(0, 10));

		assertThat(result.getContent()).containsExactly(notification);
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("SSE 구독에 필요한 알림 요약을 조회한다")
	void loadsNotificationSummary() {
		given(notificationJpaRepository.countByReceiverIdAndIsReadFalseAndDeletedAtIsNull("receiver-id"))
				.willReturn(3L);
		given(notificationJpaRepository.existsFriendRequestByReceiverId("receiver-id"))
				.willReturn(true);

		NotificationSummaryView result = adapter.loadNotificationSummary("receiver-id");

		assertThat(result.unreadCount()).isEqualTo(3L);
		assertThat(result.hasFriendRequest()).isTrue();
	}

	@Test
	@DisplayName("알림 기록과 전체 읽음 및 Diary 연관 삭제를 목적별 Repository 연산에 위임한다")
	void delegatesNotificationMutations() {
		Notification notification =
				new Notification("receiver-id", "sender-id", NotificationType.COMMENT, 10L);
		given(notificationJpaRepository.save(notification)).willReturn(notification);

		assertThat(adapter.recordNotification(notification)).isSameAs(notification);
		adapter.markAllNotificationsRead("receiver-id");
		adapter.deleteNotificationsByDiary(10L);

		then(notificationJpaRepository).should().markAllAsReadByReceiverId("receiver-id");
		then(notificationJpaRepository).should().deleteByDiaryId(10L);
	}
}
