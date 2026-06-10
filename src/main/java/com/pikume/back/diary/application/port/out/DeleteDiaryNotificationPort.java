package com.pikume.back.diary.application.port.out;

public interface DeleteDiaryNotificationPort {

	void deleteNotificationsByDiaryId(Long diaryId);
}
