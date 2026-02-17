package store.piku.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.application.port.out.SendDiaryNotificationPort;
import store.piku.back.diary.domain.Diary;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.service.NotificationService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationAdapterForDiary implements SendDiaryNotificationPort {

	private final NotificationService notificationService;

	@Override
	public void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId,
			Diary diary, RequestMetaInfo requestMetaInfo) {
		for (String friendId : friendIds) {
			if (friendId.equals(authorUserId))
				continue;

			notificationService.sendNotification(
					friendId,
					NotificationType.FRIEND_DIARY,
					authorUserId,
					diary,
					requestMetaInfo);
		}
	}
}
