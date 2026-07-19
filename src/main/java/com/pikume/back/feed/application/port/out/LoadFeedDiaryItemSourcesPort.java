package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.readmodel.FeedDiaryItemSourceView;

import java.util.Map;
import java.util.Set;

public interface LoadFeedDiaryItemSourcesPort {

	Map<Long, FeedDiaryItemSourceView> loadDiaryItemSources(Set<Long> diaryIds);
}
