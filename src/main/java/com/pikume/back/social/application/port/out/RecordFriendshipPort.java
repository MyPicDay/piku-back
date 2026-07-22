package com.pikume.back.social.application.port.out;

import com.pikume.back.social.domain.friend.Friend;

public interface RecordFriendshipPort {
	Friend establishFriendship(Friend friend);

	void removeFriendship(String userId1, String userId2);
}
