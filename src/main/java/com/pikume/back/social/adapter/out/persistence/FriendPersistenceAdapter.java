package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadFriendPort;
import com.pikume.back.social.application.port.out.LoadFriendRequestPort;
import com.pikume.back.social.application.port.out.SaveFriendPort;
import com.pikume.back.social.application.port.out.SaveFriendRequestPort;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;

import java.util.Collection;
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
	public int countByUserId(String userId) {
		return friendJpaRepository.countByUserId1OrUserId2(userId, userId);
	}

	@Override
	public List<String> findFriendIds(String userId) {
		return friendJpaRepository.findFriendIds(userId);
	}

	@Override
	public List<String> findFriendIdsWithinTargets(String userId, java.util.Set<String> targetUserIds) {
		return friendJpaRepository.findFriendIdsWithinTargets(userId, targetUserIds);
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
	public Optional<FriendRequest> findByFromUserIdAndToUserId(String fromUserId, String toUserId) {
		return friendRequestJpaRepository.findByFromUserIdAndToUserId(fromUserId, toUserId);
	}

	@Override
	public List<String> findRequestedTargetIds(String userId, Collection<String> targetUserIds) {
		return friendRequestJpaRepository.findRequestedTargetIds(userId, targetUserIds);
	}

	@Override
	public List<String> findReceivedFromUserIds(String userId, Collection<String> targetUserIds) {
		return friendRequestJpaRepository.findReceivedFromUserIds(userId, targetUserIds);
	}

	// --- SaveFriendRequestPort ---

	@Override
	public boolean saveIfAbsent(FriendRequest friendRequest) {
		try {
			friendRequestJpaRepository.insert(friendRequest.getFromUserId(), friendRequest.getToUserId());
			return true;
		} catch (DataIntegrityViolationException e) {
			return false;
		}
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
