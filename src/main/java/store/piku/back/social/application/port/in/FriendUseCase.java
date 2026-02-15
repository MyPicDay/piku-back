package store.piku.back.social.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.social.adapter.in.web.dto.FriendRemoveDTO;
import store.piku.back.social.adapter.in.web.dto.FriendRequestResponseDto;
import store.piku.back.social.adapter.in.web.dto.FriendsDTO;
import store.piku.back.social.domain.friend.vo.FriendStatus;

import java.util.List;

public interface FriendUseCase {

	boolean areFriends(String userId1, String userId2);

	FriendRequestResponseDto sendFriendRequest(String fromUserId, String toUserId, RequestMetaInfo requestMetaInfo);

	Page<FriendsDTO> findFriendList(Pageable pageable, String userId, RequestMetaInfo requestMetaInfo);

	List<String> findFriendIdList(Pageable pageable, String userId, RequestMetaInfo requestMetaInfo);

	Page<FriendsDTO> findFriendRequests(Pageable pageable, String toUserId, RequestMetaInfo requestMetaInfo);

	FriendRequestResponseDto rejectFriendRequest(String toUserId, String fromUserId);

	FriendRequestResponseDto cancelFriendRequest(String fromUserId, String toUserId);

	FriendStatus getFriendshipStatus(String currentUserId, String otherUserId);

	int countFriends(String userId);

	List<String> getFriends(String userId);

	FriendRemoveDTO removeFriend(String myId, String targetId);
}
