package store.piku.back.social.application.port.out;

import store.piku.back.social.domain.friend.FriendRequest;
import store.piku.back.social.domain.friend.vo.FriendRequestID;

public interface SaveFriendRequestPort {

	FriendRequest save(FriendRequest friendRequest);

	void delete(FriendRequest friendRequest);

	void deleteById(FriendRequestID id);
}
