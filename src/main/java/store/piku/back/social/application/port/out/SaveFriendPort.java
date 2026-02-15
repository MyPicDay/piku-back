package store.piku.back.social.application.port.out;

import store.piku.back.social.domain.friend.Friend;

public interface SaveFriendPort {

	Friend save(Friend friend);

	void deleteByUserIds(String userId1, String userId2);
}
