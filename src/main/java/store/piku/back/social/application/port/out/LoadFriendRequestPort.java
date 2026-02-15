package store.piku.back.social.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.social.domain.friend.FriendRequest;
import store.piku.back.social.domain.friend.vo.FriendRequestID;

import java.util.Optional;

public interface LoadFriendRequestPort {

	Optional<FriendRequest> findById(FriendRequestID id);

	boolean existsById(FriendRequestID id);

	Page<FriendRequest> findByToUserId(String toUserId, Pageable pageable);

	Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId);
}
