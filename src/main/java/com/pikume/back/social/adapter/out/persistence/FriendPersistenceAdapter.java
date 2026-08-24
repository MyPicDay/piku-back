package com.pikume.back.social.adapter.out.persistence;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.social.application.port.out.LoadFriendshipsPort;
import com.pikume.back.social.application.port.out.LoadPendingFriendRequestsPort;
import com.pikume.back.social.application.port.out.RecordFriendRequestPort;
import com.pikume.back.social.application.port.out.RecordFriendshipPort;
import com.pikume.back.social.application.readmodel.FriendReferenceView;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FriendPersistenceAdapter implements LoadFriendshipsPort, RecordFriendshipPort,
		LoadPendingFriendRequestsPort, RecordFriendRequestPort {
	private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
	private static final int MYSQL_DUPLICATE_KEY_ERROR_CODE = 1062;

	private final FriendJpaRepository friendJpaRepository;
	private final FriendRequestJpaRepository friendRequestJpaRepository;

	@Override
	public boolean friendshipExists(String userId1, String userId2) {
		return friendJpaRepository.existsFriendship(userId1, userId2);
	}

	@Override
	public int countFriendships(String userId) {
		return friendJpaRepository.countByUserId1OrUserId2(userId, userId);
	}

	@Override
	public List<String> loadFriendIds(String userId) {
		return friendJpaRepository.findFriendIds(userId);
	}

	@Override
	public List<String> loadFriendIdsWithin(String userId, Set<String> targetUserIds) {
		return friendJpaRepository.findFriendIdsWithinTargets(userId, targetUserIds);
	}

	@Override
	public PageResult<FriendReferenceView> loadFriendPage(String userId, PageQuery pageQuery) {
		var page = friendJpaRepository.findFriendsByUserId(userId, SpringPageMapper.toPageable(pageQuery));
		List<FriendReferenceView> content = page.getContent().stream()
				.map(friend -> new FriendReferenceView(
						friend.getUserId1().equals(userId) ? friend.getUserId2() : friend.getUserId1()))
				.toList();
		return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
	}

	@Override
	public Friend establishFriendship(Friend friend) {
		return friendJpaRepository.save(friend);
	}

	@Override
	public void removeFriendship(String userId1, String userId2) {
		friendJpaRepository.deleteByUserIds(userId1, userId2);
	}

	@Override
	public Optional<FriendRequest> loadPendingRequest(FriendRequestID requestId) {
		return friendRequestJpaRepository.findById(requestId);
	}

	@Override
	public boolean pendingRequestExists(FriendRequestID requestId) {
		return friendRequestJpaRepository.existsById(requestId);
	}

	@Override
	public List<String> loadRequestedTargetIds(String userId, Collection<String> targetUserIds) {
		return friendRequestJpaRepository.findRequestedTargetIds(userId, targetUserIds);
	}

	@Override
	public List<String> loadReceivedSenderIds(String userId, Collection<String> targetUserIds) {
		return friendRequestJpaRepository.findReceivedFromUserIds(userId, targetUserIds);
	}

	@Override
	public PageResult<FriendReferenceView> loadReceivedRequestPage(String userId, PageQuery pageQuery) {
		var page = friendRequestJpaRepository.findByToUserId(userId, SpringPageMapper.toPageable(pageQuery));
		List<FriendReferenceView> content = page.getContent().stream()
				.map(request -> new FriendReferenceView(request.getFromUserId()))
				.toList();
		return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
	}

	@Override
	public boolean tryRecordPendingRequest(FriendRequest friendRequest) {
		try {
			friendRequestJpaRepository.insert(friendRequest.getFromUserId(), friendRequest.getToUserId());
			return true;
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateKeyViolation(exception)) {
				return false;
			}
			throw exception;
		}
	}

	private boolean isDuplicateKeyViolation(DataIntegrityViolationException exception) {
		if (exception instanceof DuplicateKeyException) {
			return true;
		}
		Throwable current = exception;
		while (current != null) {
			if (current instanceof SQLException sqlException
					&& (UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())
					|| sqlException.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR_CODE)) {
				return true;
			}
			Throwable cause = current.getCause();
			current = cause == current ? null : cause;
		}
		return false;
	}

	@Override
	public void closePendingRequest(FriendRequest friendRequest) {
		friendRequestJpaRepository.delete(friendRequest);
	}

	@Override
	public void closePendingRequest(FriendRequestID requestId) {
		friendRequestJpaRepository.deleteById(requestId);
	}
}
