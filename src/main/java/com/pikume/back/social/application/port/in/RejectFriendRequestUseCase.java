package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.FriendRequestResult;

public interface RejectFriendRequestUseCase {
	FriendRequestResult rejectFriendRequest(String toUserId, String fromUserId);
}
