package com.pikume.back.notification.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.notification.application.readmodel.NotificationListView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationListViewPersistenceAdapter")
class NotificationListViewPersistenceAdapterTest {

	@InjectMocks
	private NotificationListViewPersistenceAdapter adapter;

	@Mock
	private NotificationJpaRepository notificationJpaRepository;
	@Mock
	private QueryUserSummaryUseCase queryUserSummaryUseCase;
	@Mock
	private QueryDiaryReadUseCase queryDiaryReadUseCase;
	@Mock
	private ResolveImageUrlPort resolveImageUrlPort;

	@Test
	@DisplayName("삭제된 일기와 연결된 알림은 목록에서 제외하고 발신자 요약도 조회하지 않는다")
	void excludesNotificationsForMissingDiarySummaries() {
		PageQuery pageQuery = PageQuery.of(0, 10);
		Notification activeDiaryNotification = new Notification("receiver-id", "active-sender", NotificationType.COMMENT, 1L);
		Notification deletedDiaryNotification = new Notification("receiver-id", "deleted-sender", NotificationType.COMMENT, 2L);
		Notification friendNotification = new Notification("receiver-id", "friend-sender", NotificationType.FRIEND_ACCEPT, null);

		given(notificationJpaRepository.findAllByReceiverIdAndDeletedAtIsNull(
				"receiver-id",
				PageRequest.of(0, 10)))
				.willReturn(new PageImpl<>(
						java.util.List.of(activeDiaryNotification, deletedDiaryNotification, friendNotification),
						PageRequest.of(0, 10),
						3));
		given(queryDiaryReadUseCase.getDiarySummaries(Set.of(1L, 2L)))
				.willReturn(Map.of(
						1L,
						new DiarySummaryView(
								1L,
								"owner-id",
								DiaryVisibility.PUBLIC,
								"content",
								LocalDate.of(2026, 3, 8),
								LocalDateTime.of(2026, 3, 8, 12, 0))));
		given(queryDiaryReadUseCase.getRepresentPhotoPaths(Set.of(1L, 2L))).willReturn(Map.of());
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("active-sender", "friend-sender")))
				.willReturn(Map.of(
						"active-sender", new UserSummaryView("active-sender", "활성 발신자", null),
						"friend-sender", new UserSummaryView("friend-sender", "친구 발신자", null)));

		PageResult<NotificationListView> result = adapter.loadNotifications("receiver-id", pageQuery);

		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).extracting(NotificationListView::senderNickname)
				.containsExactly("활성 발신자", "친구 발신자");
		then(queryUserSummaryUseCase).should().queryUserSummaries(Set.of("active-sender", "friend-sender"));
	}
}
