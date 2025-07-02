package store.piku.back.friend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.friend.dto.*;
import store.piku.back.friend.exception.AlreadyFriendsException;
import store.piku.back.friend.exception.FriendException;
import store.piku.back.friend.exception.FriendRequestNotFoundException;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.user.exception.UserNotFoundException;

import java.util.List;


@Tag(name = "Friend" ,description = "친구 관련 API")
@RestController
@RequestMapping("/api/relation")
@RequiredArgsConstructor
@Slf4j
public class FriendRequestController {

    private final FriendRequestService friendRequestService;


    @Operation(summary = "친구 요청,수락", description = "사용자가 다른 사용자에게 친구 요청을 보내거나, 이미 요청이 있을 경우 수락합니다.")@ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 요청 성공 혹은 수락",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FriendRequestResponseDto.class),
                            examples = {
                                    @ExampleObject(name = "요청 보냄", value = "{\"accepted\": false, \"message\": \"친구 요청을 보냈습니다.\"}"),
                                    @ExampleObject(name = "요청 수락", value = "{\"accepted\": true, \"message\": \"친구 요청을 수락했습니다.\"}")
                            }
                    )
            ),
            @ApiResponse(responseCode = "409", description = "이미 친구인 상태",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FriendRequestResponseDto.class),
                            examples = @ExampleObject(value = "{\"accepted\": false, \"message\": \"이미 친구입니다.\"}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (자신에게 요청 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FriendRequestResponseDto.class),
                            examples = @ExampleObject(value = "{\"accepted\": false, \"message\": \"자신에게 요청 할 수 없습니다.\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody FriendRequestDto requestDto) {
        log.info("친구 요청(수락) 요청 " + customUserDetails.getId() + " 가 " + requestDto.getToUserId() + "에게");
        try {
            FriendRequestResponseDto response = friendRequestService.sendFriendRequest(customUserDetails.getId(), requestDto.getToUserId());
            return ResponseEntity.ok(response);
        } catch (AlreadyFriendsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new FriendRequestResponseDto(false, e.getMessage()));
        } catch (UserNotFoundException | FriendException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new FriendRequestResponseDto(false, e.getMessage()));
        }
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 목록 반환",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FriendsDto.class)),
                            examples = @ExampleObject(value = "[" +
                                    "{\"userId\": \"user1\", \"nickname\": \"닉네임1\", \"avatar\": \"avatar1.png\"}," +
                                    "{\"userId\": \"user2\", \"nickname\": \"닉네임2\", \"avatar\": \"avatar2.png\"}" +
                                    "]")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "사용자 정보 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "null")
                    )
            )
    })
    @Operation(summary = "친구 목록 조회", description = "친구들의 id,닉네임,아바타(프로필)을 반환합니다")
    @GetMapping
    public ResponseEntity<List<FriendsDto>> getFriendList(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info(customUserDetails.getId() + " 의 친구 목록 조회 요청");
        try {
            List<FriendsDto> friends = friendRequestService.getFriendList(customUserDetails.getId());
            return ResponseEntity.ok(friends);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    @Operation(
            summary = "받은 친구 요청 목록 조회",
            description = "나에게 온 친구 요청 목록을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "친구 요청 목록 조회 성공",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FriendsDto.class)))),
                    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
            }
    )    @GetMapping("/requests")
    public ResponseEntity<List<FriendsDto>> getFriendRequests(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info(customUserDetails.getId() + " 의 받은 친구 요청 목록 조회");
        List<FriendsDto> requests = friendRequestService.getFriendRequests(customUserDetails.getId());
        return ResponseEntity.ok(requests);
    }


    @Operation(
            summary = "친구 요청 거절",
            description = "받은 친구 요청을 거절합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "친구 요청 거절 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"친구 요청을 거절했습니다.\", \"accepted\": false}"
                                    ),
                                    schema = @Schema(implementation = FriendRequestResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "친구 요청이 존재하지 않음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"해당 친구 요청을 찾을 수 없습니다.\", \"accepted\": false}"
                                    ),
                                    schema = @Schema(implementation = FriendRequestResponseDto.class)
                            )
                    )
            }
    )
    @DeleteMapping("/requests/{fromUserId}")
    public ResponseEntity<FriendRequestResponseDto> rejectFriendRequest( @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                         @PathVariable String fromUserId) {
        log.info(customUserDetails.getId() + " 가 " + fromUserId + " 의 친구 요청 거절");
        try {
            FriendRequestResponseDto response = friendRequestService.rejectFriendRequest(customUserDetails.getId(), fromUserId);
            return ResponseEntity.ok(response);
        } catch (FriendRequestNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new FriendRequestResponseDto(false, e.getMessage()));
        }
    }

    @Operation(
            summary = "친구 요청 취소",
            description = "친구 요청을 취소합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "친구 요청 취소 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"친구 요청을 취소했습니다.\", \"accepted\": false}"
                                    ),
                                    schema = @Schema(implementation = FriendRequestResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "취소할 친구 요청이 존재하지 않음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"취소할 친구 요청을 찾을 수 없습니다.\", \"accepted\": false}"
                                    ),
                                    schema = @Schema(implementation = FriendRequestResponseDto.class)
                            )
                    )
            }
    )
    @DeleteMapping("/cancel/{toUserId}")
    public ResponseEntity<FriendRequestResponseDto> cancelFriendRequest(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                          @PathVariable String toUserId) {
        log.info(customUserDetails.getId() + " 가 " + toUserId + " 에게 보낸 친구 요청 취소");
        try {
            FriendRequestResponseDto response = friendRequestService.cancelFriendRequest(customUserDetails.getId(), toUserId);
            return ResponseEntity.ok(response);
        } catch (FriendRequestNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new FriendRequestResponseDto(false, e.getMessage()));
        }
    }
}
