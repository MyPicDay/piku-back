package com.pikume.back.feed.application.port.in;

import com.pikume.back.diary.adapter.in.web.dto.ResponseDTO;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.global.dto.RequestMetaInfo;

public interface GetFeedUseCase {

	ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId);

	FeedCursorPage<ResponseDTO> getAllDiaries(FeedCursorRequest request, RequestMetaInfo requestMetaInfo, String userId);

	void logClick(String userId, Long diaryId);
}
