package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

public interface RecordFriendRequestPort {
	boolean tryRecordPendingRequest(FriendRequest friendRequest);

	void closePendingRequest(FriendRequest friendRequest);

	void closePendingRequest(FriendRequestID requestId);
}
