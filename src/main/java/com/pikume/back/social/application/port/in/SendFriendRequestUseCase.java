package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.FriendRequestResult;

public interface SendFriendRequestUseCase {
	FriendRequestResult sendFriendRequest(String fromUserId, String toUserId);
}
