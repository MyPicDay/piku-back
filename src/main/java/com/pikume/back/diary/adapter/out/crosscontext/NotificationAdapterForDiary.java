package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.DeleteDiaryNotificationPort;
import com.pikume.back.diary.application.port.out.SendDiaryNotificationPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.notification.application.port.in.NotificationUseCase;
import com.pikume.back.notification.domain.vo.NotificationType;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationAdapterForDiary implements SendDiaryNotificationPort, DeleteDiaryNotificationPort {

	private final NotificationUseCase notificationUseCase;

	@Override
	public void deleteNotificationsByDiaryId(Long diaryId) {
		notificationUseCase.deleteNotificationsByDiaryId(diaryId);
	}

	@Override
	public void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId,
			Diary diary) {
		for (String friendId : friendIds) {
			if (friendId.equals(authorUserId))
				continue;

			notificationUseCase.sendNotification(
					friendId,
					NotificationType.FRIEND_DIARY,
					authorUserId,
					diary.getId());
		}
	}
}
