package store.piku.back.diary.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.diary.application.port.out.SendDiaryNotificationPort;
import store.piku.back.diary.domain.Diary;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.notification.application.port.in.NotificationUseCase;
import store.piku.back.notification.domain.vo.NotificationType;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationAdapterForDiary implements SendDiaryNotificationPort {

	private final NotificationUseCase notificationUseCase;

	@Override
	public void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId,
			Diary diary, RequestMetaInfo requestMetaInfo) {
		for (String friendId : friendIds) {
			if (friendId.equals(authorUserId))
				continue;

			notificationUseCase.sendNotification(
					friendId,
					NotificationType.FRIEND_DIARY,
					authorUserId,
					diary.getId(),
					requestMetaInfo);
		}
	}
}
