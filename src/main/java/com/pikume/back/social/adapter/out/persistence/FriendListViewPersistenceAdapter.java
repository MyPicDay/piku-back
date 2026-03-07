package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadFriendListViewPort;
import com.pikume.back.social.application.readmodel.FriendSummaryView;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FriendListViewPersistenceAdapter implements LoadFriendListViewPort {

	private final FriendJpaRepository friendJpaRepository;
	private final FriendRequestJpaRepository friendRequestJpaRepository;
	private final UserJpaRepository userJpaRepository;

	@Override
	public Page<FriendSummaryView> loadFriendList(String userId, Pageable pageable) {
		Page<Friend> friends = friendJpaRepository.findFriendsByUserId(userId, pageable);
		Set<String> friendIds = friends.getContent().stream()
				.map(friend -> friend.getUserId1().equals(userId) ? friend.getUserId2() : friend.getUserId1())
				.collect(Collectors.toSet());
		Map<String, User> usersById = loadUsers(friendIds);

		List<FriendSummaryView> content = friends.getContent().stream()
				.map(friend -> toFriendSummaryView(friend, userId, usersById))
				.toList();
		return new PageImpl<>(content, pageable, friends.getTotalElements());
	}

	@Override
	public Page<FriendSummaryView> loadFriendRequests(String toUserId, Pageable pageable) {
		Page<FriendRequest> requests = friendRequestJpaRepository.findByToUserId(toUserId, pageable);
		Set<String> fromUserIds = requests.getContent().stream()
				.map(FriendRequest::getFromUserId)
				.collect(Collectors.toSet());
		Map<String, User> usersById = loadUsers(fromUserIds);

		List<FriendSummaryView> content = requests.getContent().stream()
				.map(request -> {
					User user = usersById.get(request.getFromUserId());
					if (user == null) {
						return new FriendSummaryView(request.getFromUserId(), "알 수 없음", null);
					}
					return new FriendSummaryView(user.getId(), user.getNickname(), user.getAvatar());
				})
				.toList();
		return new PageImpl<>(content, pageable, requests.getTotalElements());
	}

	private Map<String, User> loadUsers(Set<String> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}

		return userJpaRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
	}

	private FriendSummaryView toFriendSummaryView(Friend friend, String currentUserId, Map<String, User> usersById) {
		String friendId = friend.getUserId1().equals(currentUserId) ? friend.getUserId2() : friend.getUserId1();
		User user = usersById.get(friendId);
		if (user == null) {
			return new FriendSummaryView(friendId, "탈퇴한 사용자", null);
		}
		return new FriendSummaryView(user.getId(), user.getNickname(), user.getAvatar());
	}
}
