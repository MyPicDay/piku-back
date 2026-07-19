package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.readmodel.FeedEngagementView;

import java.util.List;
import java.util.Map;

public interface LoadFeedItemEngagementPort {

	Map<Long, FeedEngagementView> loadEngagements(String currentUserId, List<Long> diaryIds);
}
