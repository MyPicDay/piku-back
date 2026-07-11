package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.domain.Diary;

import java.util.List;

public interface SendDiaryNotificationPort {
	void notifyFriendsOfNewDiary(List<String> friendIds, String authorUserId,
			Diary diary);
}
