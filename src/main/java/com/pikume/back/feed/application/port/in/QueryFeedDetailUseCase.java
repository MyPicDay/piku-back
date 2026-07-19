package com.pikume.back.feed.application.port.in;

import com.pikume.back.feed.application.dto.FeedDiaryResult;

public interface QueryFeedDetailUseCase {

	FeedDiaryResult queryDetail(Long diaryId, String viewerId);
}
