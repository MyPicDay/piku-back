package store.piku.back.friend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.piku.back.friend.dto.FriendsDto;
import store.piku.back.friend.dto.FriendRequestResponseDto;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.entity.FriendRequest;
import store.piku.back.friend.exception.FriendException;
import store.piku.back.friend.key.FriendRequestID;
import store.piku.back.friend.repository.FriendRepository;
import store.piku.back.friend.repository.FriendRequestRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.UserService;

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



    public FriendRequestResponseDto sendFriendRequest(String fromUserId, String toUserId) {

        log.info("사용자 조회 요청");
        User fromUser = userService.getUserById(fromUserId);
        User toUser = userService.getUserById(toUserId);

        if (fromUserId.equals(toUserId)){
            throw new FriendException("자신에게 요청 할 수 없습니다.");
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
        List<FriendsDto> friends = friendRepository.findAllByUserId(user.getId());
        return friends.stream()
                .map(f -> new FriendsDto(f.getUserId(),f.getNickname(),f.getAvatar()))
                .collect(Collectors.toList());
    }
}
