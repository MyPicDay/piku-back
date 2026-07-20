package com.pikume.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.out.DeleteDiaryNotificationPort;
import com.pikume.back.diary.application.port.out.SendDiaryNotificationPort;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.RecordNotificationCommand;
import com.pikume.back.notification.application.port.in.DeleteNotificationsByDiaryUseCase;
import com.pikume.back.notification.application.port.in.RecordNotificationUseCase;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationAdapterForDiary implements SendDiaryNotificationPort, DeleteDiaryNotificationPort {

	private final RecordNotificationUseCase recordNotificationUseCase;
	private final DeleteNotificationsByDiaryUseCase deleteNotificationsByDiaryUseCase;

	@Override
	public void deleteNotificationsByDiaryId(Long diaryId) {
		deleteNotificationsByDiaryUseCase.deleteNotificationsByDiary(diaryId);
	}

	@Override
	public void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId, Long diaryId) {
		for (String friendId : friendIds) {
			if (friendId.equals(authorUserId)) {
				continue;
			}

			recordNotificationUseCase.recordNotification(new RecordNotificationCommand(
					friendId,
					NotificationKind.FRIEND_DIARY,
					authorUserId,
					diaryId));
		}
	}
}
