package store.piku.back.friend.service;

//import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import store.piku.back.friend.dto.FriendsDTO;
import store.piku.back.diary.enums.FriendStatus;
import store.piku.back.friend.dto.FriendRequestResponseDto;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.entity.FriendRequest;
import store.piku.back.friend.exception.FriendException;
import store.piku.back.friend.exception.FriendRequestNotFoundException;
import store.piku.back.friend.key.FriendRequestID;
import store.piku.back.friend.repository.FriendRepository;
import store.piku.back.friend.repository.FriendRequestRepository;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.notification.repository.NotificationRepository;
import store.piku.back.notification.service.NotificationService;
import store.piku.back.user.entity.User;
import store.piku.back.user.exception.UserNotFoundException;
import store.piku.back.user.service.reader.UserReader;
import store.piku.back.notification.entity.NotificationType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final FriendRepository friendRepository;
    private final UserReader userReader;


    public boolean areFriends(String userId1, String userId2) {
        return friendRepository.existsFriendship(userId1, userId2);
    }

    public FriendRequestResponseDto sendFriendRequest(String fromUserId, String toUserId)
    {

        log.info("사용자 조회 요청");
        User fromUser = userReader.getUserById(fromUserId);
        User toUser = userReader.getUserById(toUserId);

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

            String message = fromUser.getNickname() + "님이 친구 수락했습니다";
            notificationService.sendNotification(
                    toUser.getId(),
                    NotificationType.FRIEND,
                    message,
                    fromUserId

            );
            return new FriendRequestResponseDto(true, "친구 요청을 수락했습니다.");

        } else {
            log.info(toUserId +","+fromUserId +" 사용자 친구 요청 테이블 추가 요청");

             FriendRequest request = new FriendRequest(fromUserId, toUserId);
             friendRequestRepository.save(request);

             String message = fromUser.getNickname() + "님이 친구 요청을 보냈습니다";
             boolean alreadyNotified = notificationRepository.existsByRelatedIdAndReceiverIdAndType(
                    fromUserId, toUserId, NotificationType.FRIEND
            );


            if (!alreadyNotified) {
                notificationService.sendNotification(
                        toUser.getId(),
                        NotificationType.FRIEND,
                        message,
                        fromUserId
                );
            }
             return new FriendRequestResponseDto(false, "친구 요청을 보냈습니다.");
        }
    }

    public Page<FriendsDTO> findFriendList(Pageable pageable, String id, RequestMetaInfo requestMetaInfo) {

        log.info("사용자 친구 조회 요청");
        Page<Friend> friendsPage = friendRepository.findFriendsByUserId(id, pageable);

        Page<FriendsDTO> ret = friendsPage.map(friendEntity -> {
            String friendId = friendEntity.getUserId1().equals(id) ? friendEntity.getUserId2() : friendEntity.getUserId1();

            try {
                User friend = userReader.getUserById(friendId);
                String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(friend.getAvatar(), requestMetaInfo);
                return new FriendsDTO(friend.getId(), friend.getNickname(), avatarUrl);
            } catch (UserNotFoundException e) {
                log.warn("친구 정보 없음: {}", friendId);
                return new FriendsDTO(friendId, "탈퇴한 사용자", null);
            }
        });
        return ret;
    }

    public List<String> findFriendIdList(Pageable pageable, String userId,RequestMetaInfo requestMetaInfo) {
        Page<FriendsDTO> friendsPage = findFriendList(pageable, userId, requestMetaInfo); // requestMetaInfo 필요 없으면 null

        return friendsPage.stream()
                .map(FriendsDTO::getUserId)
                .collect(Collectors.toList());
    }



    public Page<FriendsDTO> findFriendRequests(Pageable pageable, String toUserId , RequestMetaInfo requestMetaInfo) {
        log.info("사용자에게 온 친구 요청 목록 조회: {}", toUserId);
        Page<FriendRequest> requests = friendRequestRepository.findByToUserId(toUserId,pageable);

        return requests.map(request -> {
            User fromUser = userReader.getUserById(request.getFromUserId());
            String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(fromUser.getAvatar(), requestMetaInfo);
            return new FriendsDTO(fromUser.getId(), fromUser.getNickname(), avatarUrl);
        });
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


    public FriendStatus getFriendshipStatus(String currentUserId, String otherUserId) {
        if (areFriends(currentUserId, otherUserId)) {
            return FriendStatus.FRIENDS;
        } else if (friendRequestRepository.findByFromUserIdAndToUserId(currentUserId, otherUserId).isPresent()) {
            return FriendStatus.REQUESTED; // 내가 요청함
        } else if (friendRequestRepository.findByFromUserIdAndToUserId(otherUserId, currentUserId).isPresent()) {
            return FriendStatus.RECEIVED; // 내가 받은 요청
        } else {
            return FriendStatus.NONE;
        }
    }


    /**
     * 특정 사용자의 친구 수를 반환합니다.
     * 사용자가 userId1 또는 userId2로 포함된 모든 친구 관계를 카운트합니다.
     *
     * @param userId 친구 수를 조회할 사용자의 ID
     * @return 해당 사용자의 총 친구 수
     */
    public int countFriends(String userId) {
        log.info("사용자 ID: {} 의 친구 수 조회 요청", userId);

        return friendRepository.countByUserId1OrUserId2(userId, userId);
    }
}
