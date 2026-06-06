package com.pikume.back.social.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, FriendRequestID> {
	Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId);

	Page<FriendRequest> findByToUserId(String toUserId, Pageable pageable);

	@Query("SELECT fr.toUserId FROM FriendRequest fr " +
			"WHERE fr.fromUserId = :userId AND fr.toUserId IN :targetUserIds")
	List<String> findRequestedTargetIds(@Param("userId") String userId,
			@Param("targetUserIds") Collection<String> targetUserIds);

	@Query("SELECT fr.fromUserId FROM FriendRequest fr " +
			"WHERE fr.toUserId = :userId AND fr.fromUserId IN :targetUserIds")
	List<String> findReceivedFromUserIds(@Param("userId") String userId,
			@Param("targetUserIds") Collection<String> targetUserIds);

	@Modifying
	@Query(value = "INSERT INTO friend_request (from_user_id, to_user_id) " +
			"VALUES (:fromUserId, :toUserId)", nativeQuery = true)
	int insert(@Param("fromUserId") String fromUserId, @Param("toUserId") String toUserId);
}
