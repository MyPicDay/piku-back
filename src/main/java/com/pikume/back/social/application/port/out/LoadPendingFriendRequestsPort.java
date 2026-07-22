package com.pikume.back.social.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.readmodel.FriendReferenceView;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadPendingFriendRequestsPort {
	Optional<FriendRequest> loadPendingRequest(FriendRequestID requestId);

	boolean pendingRequestExists(FriendRequestID requestId);

	List<String> loadRequestedTargetIds(String userId, Collection<String> targetUserIds);

	List<String> loadReceivedSenderIds(String userId, Collection<String> targetUserIds);

	PageResult<FriendReferenceView> loadReceivedRequestPage(String userId, PageQuery pageQuery);
}
