package com.pikume.back.feed.application.port.in;

import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;

public interface QueryFeedPageUseCase {

	FeedCursorPage<FeedDiaryResult> queryPage(FeedCursorRequest request, String viewerId);
}
