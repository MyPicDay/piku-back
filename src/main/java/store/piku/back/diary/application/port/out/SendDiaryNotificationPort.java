package store.piku.back.diary.application.port.out;

import store.piku.back.diary.domain.Diary;
import store.piku.back.global.dto.RequestMetaInfo;

import java.util.List;

public interface SendDiaryNotificationPort {
	void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId,
			Diary diary, RequestMetaInfo requestMetaInfo);
}
