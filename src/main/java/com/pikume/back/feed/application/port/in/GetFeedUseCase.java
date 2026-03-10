package com.pikume.back.feed.application.port.in;

import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.global.dto.RequestMetaInfo;

public interface GetFeedUseCase {

	FeedDiaryResult getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId);

	FeedCursorPage<FeedDiaryResult> getAllDiaries(FeedCursorRequest request, RequestMetaInfo requestMetaInfo, String userId);

	void logClick(String userId, Long diaryId);
}
