package com.pikume.back.social.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

import java.util.Optional;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, FriendRequestID> {
	Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId);

	Page<FriendRequest> findByToUserId(String toUserId, Pageable pageable);
}
