package store.piku.back.friend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.friend.dto.FriendsDto;
import store.piku.back.friend.dto.FriendRequestDto;
import store.piku.back.friend.dto.FriendRequestResponseDto;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.config.CustomUserDetails;

import java.util.List;

@Tag(name = "Friend" ,description = "친구 관련 API")
@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
@Slf4j
public class FriendRequestController {


    private final FriendRequestService friendRequestService;


    @Operation(summary = "친구 요청,수락" , description = "사용자가 다른 사용자에게 친구 요청을 보내거나, 이미 요청이 있을 경우 수락합니다.")
    @PostMapping
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody FriendRequestDto requestDto) {

        log.info("친구 요청(수락) 요청 "+ customUserDetails.getId()+ " 가 " + requestDto.getToUserId()+"에게");
        FriendRequestResponseDto savedRequest = friendRequestService.sendFriendRequest(customUserDetails.getId(), requestDto.getToUserId());
        return ResponseEntity.ok(savedRequest);
    }
    @Operation(summary = "친구 목록 조회" , description = "친구들의 id,닉네임,아바타(프로필)을 반환합니다")
    @GetMapping
    public ResponseEntity<List<FriendsDto>> getFriendList(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        log.info( customUserDetails.getId() + " 의 친구 목록 조회 요청");

        List<FriendsDto> friends = friendRequestService.getFriendList(customUserDetails.getId());
        return ResponseEntity.ok(friends);
    }

}
