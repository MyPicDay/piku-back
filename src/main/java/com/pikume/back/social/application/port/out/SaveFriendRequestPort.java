package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

public interface SaveFriendRequestPort {

	FriendRequest save(FriendRequest friendRequest);

	void delete(FriendRequest friendRequest);

	void deleteById(FriendRequestID id);
}
