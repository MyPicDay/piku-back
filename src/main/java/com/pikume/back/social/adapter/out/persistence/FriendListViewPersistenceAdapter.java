package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.social.application.port.out.LoadFriendListViewPort;
import com.pikume.back.social.application.readmodel.FriendSummaryView;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FriendListViewPersistenceAdapter implements LoadFriendListViewPort {

	private final FriendJpaRepository friendJpaRepository;
	private final FriendRequestJpaRepository friendRequestJpaRepository;
	private final QueryUserSummaryUseCase queryUserSummaryUseCase;

	@Override
	public PageResult<FriendSummaryView> loadFriendList(String userId, PageQuery pageQuery) {
		org.springframework.data.domain.Page<Friend> friends =
				friendJpaRepository.findFriendsByUserId(userId, SpringPageMapper.toPageable(pageQuery));
		Set<String> friendIds = friends.getContent().stream()
				.map(friend -> friend.getUserId1().equals(userId) ? friend.getUserId2() : friend.getUserId1())
				.collect(Collectors.toSet());
		Map<String, UserSummaryView> usersById = loadUsers(friendIds);

		List<FriendSummaryView> content = friends.getContent().stream()
				.map(friend -> toFriendSummaryView(friend, userId, usersById))
				.toList();
		return new PageResult<>(content, friends.getNumber(), friends.getSize(), friends.getTotalElements());
	}

	@Override
	public PageResult<FriendSummaryView> loadFriendRequests(String toUserId, PageQuery pageQuery) {
		org.springframework.data.domain.Page<FriendRequest> requests =
				friendRequestJpaRepository.findByToUserId(toUserId, SpringPageMapper.toPageable(pageQuery));
		Set<String> fromUserIds = requests.getContent().stream()
				.map(FriendRequest::getFromUserId)
				.collect(Collectors.toSet());
		Map<String, UserSummaryView> usersById = loadUsers(fromUserIds);

		List<FriendSummaryView> content = requests.getContent().stream()
				.map(request -> {
					UserSummaryView user = usersById.get(request.getFromUserId());
					if (user == null) {
						return new FriendSummaryView(request.getFromUserId(), "알 수 없음", null);
					}
					return new FriendSummaryView(user.id(), user.nickname(), user.avatarPath());
				})
				.toList();
		return new PageResult<>(content, requests.getNumber(), requests.getSize(), requests.getTotalElements());
	}

	private Map<String, UserSummaryView> loadUsers(Set<String> userIds) {
		return queryUserSummaryUseCase.queryUserSummaries(userIds);
	}

	private FriendSummaryView toFriendSummaryView(Friend friend, String currentUserId, Map<String, UserSummaryView> usersById) {
		String friendId = friend.getUserId1().equals(currentUserId) ? friend.getUserId2() : friend.getUserId1();
		UserSummaryView user = usersById.get(friendId);
		if (user == null) {
			return new FriendSummaryView(friendId, "탈퇴한 사용자", null);
		}
		return new FriendSummaryView(user.id(), user.nickname(), user.avatarPath());
	}
}
