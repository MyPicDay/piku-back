package com.pikume.back.social.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.social.application.dto.FriendRemovalResult;
import com.pikume.back.social.application.dto.FriendRequestResult;
import com.pikume.back.social.application.dto.FriendSummaryResult;
import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.social.application.port.out.*;
import com.pikume.back.social.application.readmodel.FriendSummaryView;
import com.pikume.back.social.domain.event.SocialEvent;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.exception.AlreadyFriendsException;
import com.pikume.back.social.domain.friend.exception.FriendException;
import com.pikume.back.social.domain.friend.exception.FriendNotFoundException;
import com.pikume.back.social.domain.friend.exception.FriendRequestNotFoundException;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService implements FriendUseCase {

	private final LoadFriendPort loadFriendPort;
	private final SaveFriendPort saveFriendPort;
	private final LoadFriendRequestPort loadFriendRequestPort;
	private final SaveFriendRequestPort saveFriendRequestPort;
	private final LoadUserInfoPort loadUserInfoPort;
	private final LoadFriendListViewPort loadFriendListViewPort;
	private final PublishEventPort publishEventPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	public boolean areFriends(String userId1, String userId2) {
		return loadFriendPort.existsFriendship(userId1, userId2);
	}

	@Override
	@Transactional
	public FriendRequestResult sendFriendRequest(String fromUserId, String toUserId,
			RequestMetaInfo requestMetaInfo) {
		log.info("사용자 조회 요청");
		loadUserInfoPort.findUserInfoById(fromUserId)
				.orElseThrow(() -> new FriendException("사용자를 찾을 수 없습니다."));
		loadUserInfoPort.findUserInfoById(toUserId)
				.orElseThrow(() -> new FriendException("사용자를 찾을 수 없습니다."));

		if (fromUserId.equals(toUserId)) {
			throw new FriendException("자신에게 요청 할 수 없습니다.");
		}

		if (areFriends(fromUserId, toUserId) || areFriends(toUserId, fromUserId)) {
			throw new AlreadyFriendsException("이미 친구입니다.");
		}

		Optional<FriendRequest> existing = loadFriendRequestPort.findById(
				new FriendRequestID(toUserId, fromUserId));

		if (existing.isPresent()) {
			log.info("{}와 {}의 요청 수락 소프트 삭제 요청", fromUserId, toUserId);
			saveFriendRequestPort.delete(existing.get());

			log.info("{},{} 사용자 친구 테이블 저장 요청", toUserId, fromUserId);
			saveFriendPort.save(new Friend(fromUserId, toUserId));

			publishEventPort.publish(new SocialEvent.FriendAcceptedEvent(toUserId, fromUserId));
			return new FriendRequestResult(true, "친구 요청을 수락했습니다.");
		}

		log.debug("{} -> {} 사용자 친구 요청", toUserId, fromUserId);
		FriendRequest request = new FriendRequest(fromUserId, toUserId);
		boolean saved = saveFriendRequestPort.saveIfAbsent(request);
		if (!saved) {
			log.debug("{}에서 {}로 보낸 친구 요청 중복 저장이 감지되었습니다.", fromUserId, toUserId);
			return friendRequestSentResult();
		}

		publishEventPort.publish(new SocialEvent.FriendRequestEvent(toUserId, fromUserId));
		return friendRequestSentResult();
	}

	@Override
	public PageResult<FriendSummaryResult> findFriendList(PageQuery pageQuery, String id, RequestMetaInfo requestMetaInfo) {
		log.info("사용자 친구 조회 요청");
		PageResult<FriendSummaryView> friendsPage = loadFriendListViewPort.loadFriendList(id, pageQuery);
		return friendsPage.map(friend -> toFriendsDto(friend, requestMetaInfo));
	}

	@Override
	public List<String> findFriendIdList(PageQuery pageQuery, String userId, RequestMetaInfo requestMetaInfo) {
		PageResult<FriendSummaryResult> friendsPage = findFriendList(pageQuery, userId, requestMetaInfo);
		return friendsPage.stream()
				.map(FriendSummaryResult::userId)
				.collect(Collectors.toList());
	}

	@Override
	public PageResult<FriendSummaryResult> findFriendRequests(PageQuery pageQuery, String toUserId, RequestMetaInfo requestMetaInfo) {
		log.info("사용자에게 온 친구 요청 목록 조회: {}", toUserId);
		PageResult<FriendSummaryView> requests = loadFriendListViewPort.loadFriendRequests(toUserId, pageQuery);
		return requests.map(friend -> toFriendsDto(friend, requestMetaInfo));
	}

	@Override
	public FriendRequestResult rejectFriendRequest(String toUserId, String fromUserId) {
		log.info("친구 요청 거절: from {} to {}", fromUserId, toUserId);
		FriendRequestID friendRequestID = new FriendRequestID(fromUserId, toUserId);
		if (!loadFriendRequestPort.existsById(friendRequestID)) {
			log.info("친구 조회 실패 : from {} to {}", fromUserId, toUserId);
			throw new FriendRequestNotFoundException("해당 친구 요청 기록을 찾을 수 없습니다.");
		}
		saveFriendRequestPort.deleteById(friendRequestID);
		return new FriendRequestResult(false, "친구 요청을 거절했습니다.");
	}

	@Override
	public FriendRequestResult cancelFriendRequest(String fromUserId, String toUserId) {
		log.info("친구 요청 취소: from {} to {}", fromUserId, toUserId);
		FriendRequestID friendRequestID = new FriendRequestID(fromUserId, toUserId);
		if (!loadFriendRequestPort.existsById(friendRequestID)) {
			log.warn("취소할 친구 요청을 찾을 수 없습니다: from {} to {}", fromUserId, toUserId);
			throw new FriendRequestNotFoundException("요청 보낸 기록이 없습니다.");
		}
		saveFriendRequestPort.deleteById(friendRequestID);
		return new FriendRequestResult(false, "친구 요청을 취소했습니다.");
	}

	@Override
	public FriendStatus getFriendshipStatus(String currentUserId, String otherUserId) {
		if (areFriends(currentUserId, otherUserId)) {
			return FriendStatus.FRIENDS;
		} else if (loadFriendRequestPort.findByFromUserIdAndToUserId(currentUserId, otherUserId).isPresent()) {
			return FriendStatus.REQUESTED;
		} else if (loadFriendRequestPort.findByFromUserIdAndToUserId(otherUserId, currentUserId).isPresent()) {
			return FriendStatus.RECEIVED;
		} else {
			return FriendStatus.NONE;
		}
	}

	@Override
	public Map<String, FriendStatus> getFriendStatuses(String currentUserId, Set<String> targetUserIds) {
		if (currentUserId == null || currentUserId.isBlank() || targetUserIds == null || targetUserIds.isEmpty()) {
			return Map.of();
		}

		Set<String> filteredTargetIds = targetUserIds.stream()
				.filter(targetUserId -> !currentUserId.equals(targetUserId))
				.collect(Collectors.toSet());
		if (filteredTargetIds.isEmpty()) {
			return Map.of();
		}

		Map<String, FriendStatus> statuses = new java.util.HashMap<>();
		loadFriendPort.findFriendIdsWithinTargets(currentUserId, filteredTargetIds)
				.forEach(friendId -> statuses.put(friendId, FriendStatus.FRIENDS));

		Set<String> unresolvedTargetIds = filteredTargetIds.stream()
				.filter(targetUserId -> !statuses.containsKey(targetUserId))
				.collect(Collectors.toSet());
		if (unresolvedTargetIds.isEmpty()) {
			return statuses;
		}

		loadFriendRequestPort.findRequestedTargetIds(currentUserId, unresolvedTargetIds)
				.forEach(targetUserId -> statuses.put(targetUserId, FriendStatus.REQUESTED));

		Set<String> requestedResolvedTargetIds = unresolvedTargetIds.stream()
				.filter(targetUserId -> !statuses.containsKey(targetUserId))
				.collect(Collectors.toSet());
		if (requestedResolvedTargetIds.isEmpty()) {
			return statuses;
		}

		loadFriendRequestPort.findReceivedFromUserIds(currentUserId, requestedResolvedTargetIds)
				.forEach(targetUserId -> statuses.put(targetUserId, FriendStatus.RECEIVED));

		return statuses;
	}

	@Override
	public int countFriends(String userId) {
		log.info("사용자 ID: {} 의 친구 수 조회 요청", userId);
		return loadFriendPort.countByUserId(userId);
	}

	@Override
	public List<String> getFriends(String userId) {
		return loadFriendPort.findFriendIds(userId);
	}

	@Override
	@Transactional
	public FriendRemovalResult removeFriend(String myId, String targetId) {
		boolean exists = loadFriendPort.existsFriendship(myId, targetId);
		if (!exists) {
			throw new FriendNotFoundException("친구 관계가 존재하지 않습니다.");
		}

		saveFriendPort.deleteByUserIds(myId, targetId);

		return new FriendRemovalResult(true, "친구 관계가 해제되었습니다.");
	}

	private FriendSummaryResult toFriendsDto(FriendSummaryView friend, RequestMetaInfo requestMetaInfo) {
		String avatarUrl = friend.avatarPath() != null
				? imagePathToUrlConverter.userAvatarImageUrl(friend.avatarPath(), requestMetaInfo)
				: null;
		return new FriendSummaryResult(friend.userId(), friend.nickname(), avatarUrl);
	}

	private FriendRequestResult friendRequestSentResult() {
		return new FriendRequestResult(false, "친구 요청을 보냈습니다.");
	}
}
