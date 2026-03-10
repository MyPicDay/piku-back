package com.pikume.back.social.application.port.in;

import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.FriendRemovalResult;
import com.pikume.back.social.application.dto.FriendRequestResult;
import com.pikume.back.social.application.dto.FriendSummaryResult;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FriendUseCase {

	boolean areFriends(String userId1, String userId2);

	FriendRequestResult sendFriendRequest(String fromUserId, String toUserId, RequestMetaInfo requestMetaInfo);

	PageResult<FriendSummaryResult> findFriendList(PageQuery pageQuery, String userId, RequestMetaInfo requestMetaInfo);

	List<String> findFriendIdList(PageQuery pageQuery, String userId, RequestMetaInfo requestMetaInfo);

	PageResult<FriendSummaryResult> findFriendRequests(PageQuery pageQuery, String toUserId, RequestMetaInfo requestMetaInfo);

	FriendRequestResult rejectFriendRequest(String toUserId, String fromUserId);

	FriendRequestResult cancelFriendRequest(String fromUserId, String toUserId);

	FriendStatus getFriendshipStatus(String currentUserId, String otherUserId);

	Map<String, FriendStatus> getFriendStatuses(String currentUserId, Set<String> targetUserIds);

	int countFriends(String userId);

	List<String> getFriends(String userId);

	FriendRemovalResult removeFriend(String myId, String targetId);
}
