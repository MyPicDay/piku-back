package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.readmodel.FeedListItemView;

import java.util.List;

public interface LoadFeedListViewPort {

	List<FeedListItemView> loadFeedListItems(List<Long> diaryIds, String currentUserId);
}
