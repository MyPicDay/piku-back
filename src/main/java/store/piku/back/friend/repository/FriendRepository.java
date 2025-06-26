package store.piku.back.friend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.piku.back.friend.dto.FriendsDto;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.key.FriendID;

import java.util.List;

@Repository
public interface FriendRepository extends JpaRepository<Friend, FriendID> {
    @Query("""
    SELECT new store.piku.back.friend.dto.FriendsDto(u.id, u.nickname, u.avatar)
    FROM Friend f
    JOIN User u ON (
        CASE
            WHEN f.userId1 = :userId THEN f.userId2
            ELSE f.userId1
        END = u.id
    )
    WHERE f.userId1 = :userId OR f.userId2 = :userId
""")    List<FriendsDto> findAllByUserId(@Param("userId") String userId);
}
