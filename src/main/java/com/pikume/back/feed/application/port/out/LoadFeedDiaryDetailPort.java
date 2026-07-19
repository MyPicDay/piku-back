package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;

import java.util.Optional;

public interface LoadFeedDiaryDetailPort {

	Optional<FeedDiaryDetailView> loadVisibleDiary(Long diaryId, String viewerId);
}
