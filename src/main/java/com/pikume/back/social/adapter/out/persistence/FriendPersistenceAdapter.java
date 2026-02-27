package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadFriendPort;
import com.pikume.back.social.application.port.out.LoadFriendRequestPort;
import com.pikume.back.social.application.port.out.SaveFriendPort;
import com.pikume.back.social.application.port.out.SaveFriendRequestPort;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FriendPersistenceAdapter implements LoadFriendPort, SaveFriendPort,
		LoadFriendRequestPort, SaveFriendRequestPort {

	private final FriendJpaRepository friendJpaRepository;
	private final FriendRequestJpaRepository friendRequestJpaRepository;

	// --- LoadFriendPort ---

	@Override
	public boolean existsFriendship(String userId1, String userId2) {
		return friendJpaRepository.existsFriendship(userId1, userId2);
	}

	@Override
	public Page<Friend> findFriendsByUserId(String userId, Pageable pageable) {
		return friendJpaRepository.findFriendsByUserId(userId, pageable);
	}

	@Override
	public int countByUserId(String userId) {
		return friendJpaRepository.countByUserId1OrUserId2(userId, userId);
	}

	@Override
	public List<String> findFriendIds(String userId) {
		return friendJpaRepository.findFriendIds(userId);
	}

	// --- SaveFriendPort ---

	@Override
	public Friend save(Friend friend) {
		return friendJpaRepository.save(friend);
	}

	@Override
	public void deleteByUserIds(String userId1, String userId2) {
		friendJpaRepository.deleteByUserIds(userId1, userId2);
	}

	// --- LoadFriendRequestPort ---

	@Override
	public Optional<FriendRequest> findById(FriendRequestID id) {
		return friendRequestJpaRepository.findById(id);
	}

	@Override
	public boolean existsById(FriendRequestID id) {
		return friendRequestJpaRepository.existsById(id);
	}

	@Override
	public Page<FriendRequest> findByToUserId(String toUserId, Pageable pageable) {
		return friendRequestJpaRepository.findByToUserId(toUserId, pageable);
	}

	@Override
	public Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId) {
		return friendRequestJpaRepository.findByFromUserIdAndToUserId(fromUserId, toUserId);
	}

	// --- SaveFriendRequestPort ---

	@Override
	public FriendRequest save(FriendRequest friendRequest) {
		return friendRequestJpaRepository.save(friendRequest);
	}

	@Override
	public void delete(FriendRequest friendRequest) {
		friendRequestJpaRepository.delete(friendRequest);
	}

	@Override
	public void deleteById(FriendRequestID id) {
		friendRequestJpaRepository.deleteById(id);
	}
}
