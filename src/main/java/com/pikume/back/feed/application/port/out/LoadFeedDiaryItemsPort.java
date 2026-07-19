package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.readmodel.FeedDiaryItemSourceView;

import java.util.Map;
import java.util.Set;

public interface LoadFeedDiaryItemsPort {

	Map<Long, FeedDiaryItemSourceView> loadDiaryItems(Set<Long> diaryIds);
}
