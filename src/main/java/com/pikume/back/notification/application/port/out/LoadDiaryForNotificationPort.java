package com.pikume.back.notification.application.port.out;

public interface LoadDiaryForNotificationPort {

	record DiaryNotificationInfo(
			Long diaryId,
			String thumbnailUrl,
			boolean anonymous
	) {
	}

	String getDiaryThumbnailUrl(Long diaryId);

	DiaryNotificationInfo getDiaryNotificationInfo(Long diaryId);
}
