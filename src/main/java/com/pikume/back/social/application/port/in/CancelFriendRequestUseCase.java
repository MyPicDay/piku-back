package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.FriendRequestResult;

public interface CancelFriendRequestUseCase {
	FriendRequestResult cancelFriendRequest(String fromUserId, String toUserId);
}
