package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedFriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadFeedFriendshipPort {

	List<String> loadFriendUserIds(String userId);

	Map<String, FeedFriendStatus> loadFriendStatuses(String currentUserId, Set<String> targetUserIds);
}
