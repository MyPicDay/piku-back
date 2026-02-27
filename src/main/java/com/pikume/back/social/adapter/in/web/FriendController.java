package com.pikume.back.social.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.social.adapter.in.web.dto.*;
import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.social.domain.friend.exception.FriendException;
import com.pikume.back.social.domain.friend.exception.FriendNotFoundException;
import com.pikume.back.social.domain.friend.exception.FriendRequestNotFoundException;

@Tag(name = "Friend", description = "친구 관련 API")
@RestController
@RequestMapping("/api/relation")
@RequiredArgsConstructor
@Slf4j
public class FriendController {

	private final FriendUseCase friendUseCase;
	private final RequestMetaMapper requestMetaMapper;

	@Operation(summary = "친구 요청,수락", description = "사용자가 다른 사용자에게 친구 요청을 보내거나, 이미 요청이 있을 경우 수락합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "친구 요청 성공 혹은 수락", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FriendRequestResponseDto.class), examples = {
					@ExampleObject(name = "요청 보냄", value = "{\"accepted\": false, \"message\": \"친구 요청을 보냈습니다.\"}"),
					@ExampleObject(name = "요청 수락", value = "{\"accepted\": true, \"message\": \"친구 요청을 수락했습니다.\"}")
			})),
			@ApiResponse(responseCode = "409", description = "이미 친구인 상태", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FriendRequestResponseDto.class), examples = @ExampleObject(value = "{\"accepted\": false, \"message\": \"이미 친구입니다.\"}"))),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 (자신에게 요청 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FriendRequestResponseDto.class), examples = @ExampleObject(value = "{\"accepted\": false, \"message\": \"자신에게 요청 할 수 없습니다.\"}")))
	})
	@PostMapping
	public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
			@AuthenticationPrincipal CustomUserDetails customUserDetails,
			@RequestBody FriendRequestDto requestDto,
			HttpServletRequest request) {
		log.info("친구 요청(수락) 요청 {} 가 {}에게", customUserDetails.getId(), requestDto.getToUserId());
		try {
			RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
			FriendRequestResponseDto response = friendUseCase.sendFriendRequest(customUserDetails.getId(),
					requestDto.getToUserId(), requestMetaInfo);
			return ResponseEntity.ok(response);
		} catch (FriendException e) {
			return ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body(new FriendRequestResponseDto(false, e.getMessage()));
		}
	}

	@Operation(summary = "친구 목록 조회", description = "친구들의 id,닉네임,아바타(프로필)을 반환합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "친구 목록 반환"),
			@ApiResponse(responseCode = "404", description = "사용자 정보 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "null")))
	})
	@GetMapping
	public ResponseEntity<Page<FriendsDTO>> findFriendList(
			@ParameterObject @PageableDefault(sort = "userId1", direction = Sort.Direction.DESC) Pageable pageable,
			@AuthenticationPrincipal CustomUserDetails customUserDetails, HttpServletRequest request) {
		log.info("{} 의 친구 목록 조회 요청", customUserDetails.getId());

		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		Page<FriendsDTO> friends = friendUseCase.findFriendList(pageable, customUserDetails.getId(), requestMetaInfo);

		return ResponseEntity.ok(friends);
	}

	@Operation(summary = "받은 요청 목록 조회", description = "나에게 온 친구 요청 목록을 조회합니다.", responses = {
			@ApiResponse(responseCode = "200", description = "친구 요청 목록 조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FriendsDTO.class)))),
			@ApiResponse(responseCode = "204", description = "받은 요청 없음 (No Content)"),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
	})
	@GetMapping("/requests")
	public ResponseEntity<Page<FriendsDTO>> findFriendRequests(
			@ParameterObject @PageableDefault Pageable pageable, @AuthenticationPrincipal CustomUserDetails customUserDetails,
			HttpServletRequest request) {
		log.info("{} 의 받은 친구 요청 목록 조회", customUserDetails.getId());

		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		Page<FriendsDTO> requests = friendUseCase.findFriendRequests(pageable, customUserDetails.getId(), requestMetaInfo);

		return ResponseEntity.ok(requests);
	}

	@Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.", responses = {
			@ApiResponse(responseCode = "200", description = "친구 요청 거절 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"친구 요청을 거절했습니다.\", \"accepted\": false}"), schema = @Schema(implementation = FriendRequestResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "친구 요청이 존재하지 않음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"해당 친구 요청을 찾을 수 없습니다.\", \"accepted\": false}"), schema = @Schema(implementation = FriendRequestResponseDto.class)))
	})
	@DeleteMapping("/requests/{fromUserId}")
	public ResponseEntity<FriendRequestResponseDto> rejectFriendRequest(
			@AuthenticationPrincipal CustomUserDetails customUserDetails,
			@PathVariable String fromUserId) {
		log.info("{} 가 {} 의 친구 요청 거절", customUserDetails.getId(), fromUserId);
		try {
			FriendRequestResponseDto response = friendUseCase.rejectFriendRequest(customUserDetails.getId(), fromUserId);
			return ResponseEntity.ok(response);
		} catch (FriendRequestNotFoundException e) {
			return ResponseEntity
					.status(HttpStatus.NOT_FOUND)
					.body(new FriendRequestResponseDto(false, e.getMessage()));
		}
	}

	@Operation(summary = "친구 요청 취소", description = "친구 요청을 취소합니다.", responses = {
			@ApiResponse(responseCode = "200", description = "친구 요청 취소 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"친구 요청을 취소했습니다.\", \"accepted\": false}"), schema = @Schema(implementation = FriendRequestResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "취소할 친구 요청이 존재하지 않음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"취소할 친구 요청을 찾을 수 없습니다.\", \"accepted\": false}"), schema = @Schema(implementation = FriendRequestResponseDto.class)))
	})
	@DeleteMapping("/cancel/{toUserId}")
	public ResponseEntity<FriendRequestResponseDto> cancelFriendRequest(
			@AuthenticationPrincipal CustomUserDetails customUserDetails,
			@PathVariable String toUserId) {
		log.info("{} 가 {} 에게 보낸 친구 요청 취소", customUserDetails.getId(), toUserId);
		try {
			FriendRequestResponseDto response = friendUseCase.cancelFriendRequest(customUserDetails.getId(), toUserId);
			return ResponseEntity.ok(response);
		} catch (FriendRequestNotFoundException e) {
			return ResponseEntity
					.status(HttpStatus.NOT_FOUND)
					.body(new FriendRequestResponseDto(false, e.getMessage()));
		}
	}

	@Operation(summary = "친구 끊기", description = "특정 사용자의 친구 관계를 삭제합니다.", responses = {
			@ApiResponse(responseCode = "200", description = "친구 관계 삭제 성공"),
			@ApiResponse(responseCode = "404", description = "친구 관계가 존재하지 않을 경우", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FriendRemoveDTO.class, example = "{\"success\": false, \"message\": \"친구 관계가 존재하지 않습니다.\"}")))
	})
	@DeleteMapping("/{toUserId}")
	public ResponseEntity<FriendRemoveDTO> removeFriend(
			@AuthenticationPrincipal CustomUserDetails customUserDetails,
			@PathVariable String toUserId) {

		String fromUserId = customUserDetails.getId();
		log.info("User {} is unfriending user {}", fromUserId, toUserId);

		try {
			FriendRemoveDTO response = friendUseCase.removeFriend(fromUserId, toUserId);
			return ResponseEntity.ok(response);
		} catch (FriendNotFoundException e) {
			return ResponseEntity
					.status(HttpStatus.NOT_FOUND)
					.body(new FriendRemoveDTO(false, e.getMessage()));
		}
	}
}
