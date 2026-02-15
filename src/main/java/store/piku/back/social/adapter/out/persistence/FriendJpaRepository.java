package store.piku.back.social.adapter.out.persistence;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.piku.back.social.domain.friend.Friend;
import store.piku.back.social.domain.friend.vo.FriendID;

import java.util.List;

public interface FriendJpaRepository extends JpaRepository<Friend, FriendID> {

	@Query("SELECT f FROM Friend f WHERE f.userId1 = :userId OR f.userId2 = :userId")
	Page<Friend> findFriendsByUserId(@Param("userId") String userId, Pageable pageable);

	@Query("SELECT CASE " +
			"WHEN f.userId1 = :userId THEN f.userId2 " +
			"ELSE f.userId1 END " +
			"FROM Friend f " +
			"WHERE f.userId1 = :userId OR f.userId2 = :userId")
	List<String> findFriendIds(@Param("userId") String userId);

	@Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friend f " +
			"WHERE ((f.userId1 = :userId1 AND f.userId2 = :userId2) OR (f.userId1 = :userId2 AND f.userId2 = :userId1)) ")
	boolean existsFriendship(String userId1, String userId2);

	int countByUserId1OrUserId2(String userId1, String userId2);

	@Modifying
	@Transactional
	@Query("DELETE FROM Friend f " +
			"WHERE (f.userId1 = :userId1 AND f.userId2 = :userId2) " +
			"   OR (f.userId1 = :userId2 AND f.userId2 = :userId1)")
	void deleteByUserIds(@Param("userId1") String userId1,
			@Param("userId2") String userId2);
}
