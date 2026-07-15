package com.pikume.back.diary.application.port.out;

import java.util.List;

public interface SendDiaryNotificationPort {
	void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId, Long diaryId);
}
