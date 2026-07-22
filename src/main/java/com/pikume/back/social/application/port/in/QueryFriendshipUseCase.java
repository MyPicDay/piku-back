package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.FriendshipStatusResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryFriendshipUseCase {
	boolean areFriends(String userId1, String userId2);

	FriendshipStatusResult queryFriendshipStatus(String currentUserId, String otherUserId);

	Map<String, FriendshipStatusResult> queryFriendshipStatuses(String currentUserId, Set<String> targetUserIds);

	int queryFriendCount(String userId);

	List<String> queryFriendIds(String userId);
}
