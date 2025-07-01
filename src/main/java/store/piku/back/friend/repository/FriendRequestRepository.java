package store.piku.back.friend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.piku.back.friend.entity.FriendRequest;
import store.piku.back.friend.key.FriendRequestID;

import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, FriendRequestID> {
    Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId);
}
