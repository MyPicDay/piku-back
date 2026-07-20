package com.pikume.back.diary.adapter.out.crosscontext;

import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.RecordNotificationCommand;
import com.pikume.back.notification.application.port.in.DeleteNotificationsByDiaryUseCase;
import com.pikume.back.notification.application.port.in.RecordNotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationAdapterForDiary")
class NotificationAdapterForDiaryTest {

	@InjectMocks
	private NotificationAdapterForDiary adapter;

	@Mock
	private RecordNotificationUseCase recordNotificationUseCase;
	@Mock
	private DeleteNotificationsByDiaryUseCase deleteNotificationsByDiaryUseCase;

	@Test
	@DisplayName("친구에게 새 Diary 알림 기록을 요청하고 작성자 자신은 제외한다")
	void recordsFriendDiaryNotifications() {
		adapter.notifyFriendsOfNewDiary(
				List.of("author-id", "friend-id"),
				"author-id",
				10L);

		ArgumentCaptor<RecordNotificationCommand> commandCaptor =
				ArgumentCaptor.forClass(RecordNotificationCommand.class);
		then(recordNotificationUseCase).should().recordNotification(commandCaptor.capture());
		assertThat(commandCaptor.getValue()).isEqualTo(new RecordNotificationCommand(
				"friend-id",
				NotificationKind.FRIEND_DIARY,
				"author-id",
				10L));
	}

	@Test
	@DisplayName("Diary 삭제 시 연관 Notification 삭제를 요청한다")
	void deletesNotificationsByDiary() {
		adapter.deleteNotificationsByDiaryId(10L);

		then(deleteNotificationsByDiaryUseCase).should().deleteNotificationsByDiary(10L);
	}
}
