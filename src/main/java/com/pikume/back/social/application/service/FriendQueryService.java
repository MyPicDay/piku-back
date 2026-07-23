package com.pikume.back.social.application.service;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.dto.FriendSummaryResult;
import com.pikume.back.social.application.dto.FriendshipStatusResult;
import com.pikume.back.social.application.port.in.QueryFriendPageUseCase;
import com.pikume.back.social.application.port.in.QueryFriendshipUseCase;
import com.pikume.back.social.application.port.out.LoadFriendshipsPort;
import com.pikume.back.social.application.port.out.LoadPendingFriendRequestsPort;
import com.pikume.back.social.application.port.out.LoadSocialParticipantProfilesPort;
import com.pikume.back.social.application.readmodel.FriendReferenceView;
import com.pikume.back.social.application.readmodel.SocialParticipantProfile;
import com.pikume.back.social.domain.friend.vo.FriendRequestID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendQueryService implements QueryFriendPageUseCase, QueryFriendshipUseCase {

	private final LoadFriendshipsPort loadFriendshipsPort;
	private final LoadPendingFriendRequestsPort loadPendingFriendRequestsPort;
	private final LoadSocialParticipantProfilesPort loadSocialParticipantProfilesPort;

	@Override
	public PageResult<FriendSummaryResult> queryFriendPage(PageQuery pageQuery, String userId) {
		return enrich(loadFriendshipsPort.loadFriendPage(userId, pageQuery), "탈퇴한 사용자");
	}

	@Override
	public PageResult<FriendSummaryResult> queryReceivedFriendRequestPage(PageQuery pageQuery, String userId) {
		return enrich(loadPendingFriendRequestsPort.loadReceivedRequestPage(userId, pageQuery), "알 수 없음");
	}

	@Override
	public boolean areFriends(String userId1, String userId2) {
		return loadFriendshipsPort.friendshipExists(userId1, userId2);
	}

	@Override
	public FriendshipStatusResult queryFriendshipStatus(String currentUserId, String otherUserId) {
		if (areFriends(currentUserId, otherUserId)) {
			return FriendshipStatusResult.FRIENDS;
		}
		if (loadPendingFriendRequestsPort.pendingRequestExists(new FriendRequestID(currentUserId, otherUserId))) {
			return FriendshipStatusResult.REQUESTED;
		}
		if (loadPendingFriendRequestsPort.pendingRequestExists(new FriendRequestID(otherUserId, currentUserId))) {
			return FriendshipStatusResult.RECEIVED;
		}
		return FriendshipStatusResult.NONE;
	}

	@Override
	public Map<String, FriendshipStatusResult> queryFriendshipStatuses(
			String currentUserId, Set<String> targetUserIds) {
		if (currentUserId == null || currentUserId.isBlank() || targetUserIds == null || targetUserIds.isEmpty()) {
			return Map.of();
		}
		Set<String> targets = targetUserIds.stream()
				.filter(target -> !currentUserId.equals(target))
				.collect(Collectors.toSet());
		if (targets.isEmpty()) {
			return Map.of();
		}
		Map<String, FriendshipStatusResult> statuses = new HashMap<>();
		loadFriendshipsPort.loadFriendIdsWithin(currentUserId, targets)
				.forEach(id -> statuses.put(id, FriendshipStatusResult.FRIENDS));

		Set<String> unresolved = unresolved(targets, statuses);
		if (!unresolved.isEmpty()) {
			loadPendingFriendRequestsPort.loadRequestedTargetIds(currentUserId, unresolved)
					.forEach(id -> statuses.put(id, FriendshipStatusResult.REQUESTED));
		}
		unresolved = unresolved(targets, statuses);
		if (!unresolved.isEmpty()) {
			loadPendingFriendRequestsPort.loadReceivedSenderIds(currentUserId, unresolved)
					.forEach(id -> statuses.put(id, FriendshipStatusResult.RECEIVED));
		}
		return statuses;
	}

	@Override
	public int queryFriendCount(String userId) {
		return loadFriendshipsPort.countFriendships(userId);
	}

	@Override
	public List<String> queryFriendIds(String userId) {
		return loadFriendshipsPort.loadFriendIds(userId);
	}

	private PageResult<FriendSummaryResult> enrich(PageResult<FriendReferenceView> page, String missingNickname) {
		Set<String> ids = page.content().stream().map(FriendReferenceView::userId).collect(Collectors.toSet());
		Map<String, SocialParticipantProfile> profiles = loadSocialParticipantProfilesPort.loadProfiles(ids);
		return page.map(reference -> {
			SocialParticipantProfile profile = profiles.get(reference.userId());
			return profile != null
					? new FriendSummaryResult(profile.userId(), profile.nickname(), profile.avatarUrl())
					: new FriendSummaryResult(reference.userId(), missingNickname, null);
		});
	}

	private Set<String> unresolved(Set<String> targets, Map<String, FriendshipStatusResult> statuses) {
		return targets.stream().filter(target -> !statuses.containsKey(target)).collect(Collectors.toSet());
	}
}
