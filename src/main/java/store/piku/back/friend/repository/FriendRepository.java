package store.piku.back.friend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.key.FriendID;

import java.util.List;


@Repository
public interface FriendRepository extends JpaRepository<Friend, FriendID> {

    @Query("SELECT f FROM Friend f WHERE f.userId1 = :userId OR f.userId2 = :userId")
    Page<Friend> findFriendsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT CASE " +
            "WHEN f.userId1 = :userId THEN f.userId2 " +
            "ELSE f.userId1 END " +
            "FROM Friend f " +
            "WHERE f.userId1 = :userId OR f.userId2 = :userId")
    List<String> findFriendIds(@Param("userId") String userId);


    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friend f " +
            "WHERE ((f.userId1 = :userId1 AND f.userId2 = :userId2) OR (f.userId1 = :userId2 AND f.userId2 = :userId1)) "
            )
    boolean existsFriendship(String userId1, String userId2);

    int countByUserId1OrUserId2(String userId1, String userId2);

}