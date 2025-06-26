package store.piku.back.friend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.key.FriendID;

@Repository
public interface FriendRepository extends JpaRepository<Friend, FriendID> {
}
