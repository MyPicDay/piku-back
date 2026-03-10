package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadFriendRequestPort {

	Optional<FriendRequest> findById(FriendRequestID id);

	boolean existsById(FriendRequestID id);

	Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId);

	List<String> findRequestedTargetIds(String userId, Collection<String> targetUserIds);

	List<String> findReceivedFromUserIds(String userId, Collection<String> targetUserIds);
}
