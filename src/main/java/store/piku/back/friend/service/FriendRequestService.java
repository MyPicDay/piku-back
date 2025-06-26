package store.piku.back.friend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.piku.back.friend.dto.FriendRequestResponseDto;
import store.piku.back.friend.entity.Friend;
import store.piku.back.friend.entity.FriendRequest;
import store.piku.back.friend.key.FriendRequestID;
import store.piku.back.friend.repository.FriendRepository;
import store.piku.back.friend.repository.FriendRequestRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;

    public FriendRequestResponseDto sendFriendRequest(String fromUserId, String toUserId) {
        Optional<FriendRequest> existing = friendRequestRepository.findById(new FriendRequestID(toUserId, fromUserId));

        if (existing.isPresent()) {
            friendRequestRepository.delete(existing.get());
            friendRepository.save(new Friend(fromUserId, toUserId));
            return new FriendRequestResponseDto(true, "친구 요청을 수락했습니다.");

        } else {
            FriendRequest request = new FriendRequest(fromUserId, toUserId);
             friendRequestRepository.save(request);
             return new FriendRequestResponseDto(false, "친구 요청을 보냈습니다.");

        }
    }
}
