package com.pikume.back.feed.application.port.in;

import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;

public interface GetFeedUseCase {

	FeedDiaryResult getDiaryWithPhotos(Long diaryId, String userId);

	FeedCursorPage<FeedDiaryResult> getAllDiaries(FeedCursorRequest request, String userId);

	void logClick(String userId, Long diaryId);
}
