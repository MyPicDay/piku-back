package store.piku.back.friend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.piku.back.friend.dto.FriendsDto;
import store.piku.back.friend.dto.FriendRequestResponseDto;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.entity.FriendRequest;
import store.piku.back.friend.exception.FriendException;
import store.piku.back.friend.exception.FriendRequestNotFoundException;
import store.piku.back.friend.key.FriendRequestID;
import store.piku.back.friend.repository.FriendRepository;
import store.piku.back.friend.repository.FriendRequestRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.exception.UserNotFoundException;
import store.piku.back.user.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final UserService userService;


    public boolean areFriends(String userId1, String userId2) {
        return friendRepository.existsFriendship(userId1, userId2);
    }

    public FriendRequestResponseDto sendFriendRequest(String fromUserId, String toUserId) {

        log.info("사용자 조회 요청");
        User fromUser = userService.getUserById(fromUserId);
        User toUser = userService.getUserById(toUserId);

        if (fromUserId.equals(toUserId)){
            throw new FriendException("자신에게 요청 할 수 없습니다.");
        }

        if (areFriends(fromUserId, toUserId) || areFriends(toUserId, fromUserId)) {
            throw new FriendException("이미 친구입니다.");
        }

        Optional<FriendRequest> existing = friendRequestRepository.findById(new FriendRequestID(toUser.getId(), fromUser.getId()));

        if (existing.isPresent()) {

            log.info(fromUserId + "와"+toUserId +"의 요청 수락 소프트 삭제 요청");
            friendRequestRepository.delete(existing.get());

            log.info(toUserId +","+fromUserId +" 사용자 친구 테이블 저장 요청");
            friendRepository.save(new Friend(fromUserId, toUserId));
            return new FriendRequestResponseDto(true, "친구 요청을 수락했습니다.");

        } else {
            log.info(toUserId +","+fromUserId +" 사용자 친구 요청 테이블 추가 요청");

             FriendRequest request = new FriendRequest(fromUserId, toUserId);
             friendRequestRepository.save(request);
             return new FriendRequestResponseDto(false, "친구 요청을 보냈습니다.");

        }
    }

    public List<FriendsDto> getFriendList(String id) {

        log.info("사용자 조회 요청");
        User user = userService.getUserById(id);

        log.info("사용자 친구 조회 요청");
        List<String> friends_id = friendRepository.findFriendIds(user.getId());

        List<FriendsDto> friends = new ArrayList<>();

        for (String friendsId : friends_id) {
            try {
                User friend = userService.getUserById(friendsId);
                friends.add(new FriendsDto(friend.getId(), friend.getNickname(), friend.getAvatar()));
            } catch (UserNotFoundException e) {
                log.warn("친구 정보 없음: {}", id);
            }
        }
        return friends;
    }

    public List<FriendsDto> getFriendRequests(String toUserId) {
        log.info("사용자에게 온 친구 요청 목록 조회: {}", toUserId);
        List<FriendRequest> requests = friendRequestRepository.findByToUserId(toUserId);
        return requests.stream()
                .map(request -> {
                    User fromUser = userService.getUserById(request.getFromUserId());
                    return new FriendsDto(fromUser.getId(), fromUser.getNickname(), fromUser.getAvatar());
                })
                .collect(Collectors.toList());
    }

    public FriendRequestResponseDto rejectFriendRequest(String toUserId, String fromUserId) {
        log.info("친구 요청 거절: from {} to {}", fromUserId, toUserId);
        FriendRequestID friendRequestID = new FriendRequestID(fromUserId, toUserId);
        if (!friendRequestRepository.existsById(friendRequestID)) {
            log.info("친구 조회 실패 : from {} to {}", fromUserId, toUserId);
            throw new FriendRequestNotFoundException("해당 친구 요청 기록을 찾을 수 없습니다.");
        }
        friendRequestRepository.deleteById(friendRequestID);
        return new FriendRequestResponseDto(false, "친구 요청을 거절했습니다.");
    }



    public FriendRequestResponseDto cancelFriendRequest(String fromUserId, String toUserId) {
        log.info("친구 요청 취소: from {} to {}", fromUserId, toUserId);
        FriendRequestID friendRequestID = new FriendRequestID(fromUserId, toUserId);
        if (!friendRequestRepository.existsById(friendRequestID)) {
            log.warn("취소할 친구 요청을 찾을 수 없습니다: from {} to {}", fromUserId, toUserId);
            throw new FriendRequestNotFoundException("요청 보낸 기록이 없습니다.");
        }
        friendRequestRepository.deleteById(friendRequestID);
        return new FriendRequestResponseDto(false, "친구 요청을 취소했습니다.");
    }
}
