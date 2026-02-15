package store.piku.back.user.adapter.out.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.user.application.port.out.UserFriendPort;

/**
 * 친구 도메인 어댑터
 * User 도메인에서 친구 정보를 조회하기 위한 Anti-Corruption Layer입니다.
 */
@Component
@RequiredArgsConstructor
public class FriendAdapterForUser implements UserFriendPort {

	private final FriendRequestService friendRequestService;

	@Override
	public int countFriends(String userId) {
		return friendRequestService.countFriends(userId);
	}

	@Override
	public String getFriendshipStatus(String userId, String targetUserId) {
		return friendRequestService.getFriendshipStatus(userId, targetUserId).name();
	}
}
