package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.friend.Friend;

public interface SaveFriendPort {

	Friend save(Friend friend);

	void deleteByUserIds(String userId1, String userId2);
}
