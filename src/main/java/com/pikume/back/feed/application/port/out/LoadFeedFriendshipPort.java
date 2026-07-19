package com.pikume.back.feed.application.port.out;

import java.util.List;

public interface LoadFeedFriendshipPort {

	List<String> loadFriendUserIds(String userId);
}
