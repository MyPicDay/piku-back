package store.piku.back.friend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.friend.dto.FriendRequestDto;
import store.piku.back.friend.dto.FriendRequestResponseDto;
import store.piku.back.friend.entity.FriendRequest;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.config.CustomUserDetails;


@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendRequestController {


    private final FriendRequestService friendRequestService;


    @PostMapping
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody FriendRequestDto requestDto) {

        FriendRequestResponseDto savedRequest = friendRequestService.sendFriendRequest(customUserDetails.getId(), requestDto.getToUserId());
        return ResponseEntity.ok(savedRequest);
    }
}
