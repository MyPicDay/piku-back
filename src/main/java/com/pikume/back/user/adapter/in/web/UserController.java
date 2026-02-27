package com.pikume.back.user.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.user.adapter.in.web.dto.request.UpdateProfileRequest;
import com.pikume.back.user.adapter.in.web.dto.response.NicknameChangeResponse;
import com.pikume.back.user.adapter.in.web.dto.response.NicknameCheckResponse;
import com.pikume.back.user.adapter.in.web.dto.response.ProfilePreviewResponse;
import com.pikume.back.user.adapter.in.web.dto.response.UserProfileResponse;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UpdateProfileCommand;
import com.pikume.back.user.application.dto.UpdateProfileResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.port.in.CheckNicknameUseCase;
import com.pikume.back.user.application.port.in.GetUserProfileUseCase;
import com.pikume.back.user.application.port.in.UpdateProfileUseCase;

@Tag(name = "Users", description = "유저 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final GetUserProfileUseCase getUserProfileUseCase;
	private final UpdateProfileUseCase updateProfileUseCase;
	private final CheckNicknameUseCase checkNicknameUseCase;
	private final RequestMetaMapper requestMetaMapper;

	@Operation(summary = "프로필 미리보기 정보 반환", description = "사용자의 프로필 미리보기 시 사용될 정보를 조회하여 반환합니다.")
	@GetMapping("/{userId}/profile-preview")
	public ResponseEntity<ProfilePreviewResponse> getProfilePreview(
			@PathVariable String userId,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			HttpServletRequest request) {
		log.info("사용자 {}의 프로필 미리보기 조회 요청", userId);

		RequestMetaInfo meta = requestMetaMapper.extractMetaInfo(request);
		String loginUserId = userDetails != null ? userDetails.getId() : null;
		ProfilePreviewResult result = getUserProfileUseCase.getProfilePreview(userId, loginUserId, meta);

		return ResponseEntity.ok(ProfilePreviewResponse.from(result));
	}

	@Operation(summary = "사용자 프로필 조회")
	@GetMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> getUserProfile(
			@PathVariable String userId,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			HttpServletRequest request) {
		RequestMetaInfo meta = requestMetaMapper.extractMetaInfo(request);
		UserProfileResult result = getUserProfileUseCase.getUserProfile(userId, userDetails.getId(), meta);

		return ResponseEntity.ok(UserProfileResponse.from(result));
	}

	@Operation(summary = "닉네임 중복조회 검사", responses = {
			@ApiResponse(responseCode = "200", description = "사용 가능한 닉네임입니다."),
			@ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임입니다.")
	})
	@GetMapping("/nickname/availability")
	public ResponseEntity<NicknameCheckResponse> checkNickname(
			@RequestParam String nickname,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		boolean reserved = checkNicknameUseCase.checkAvailability(nickname, userDetails.getId());
		NicknameCheckResponse response = new NicknameCheckResponse(
				reserved,
				reserved ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.");
		return ResponseEntity.status(reserved ? HttpStatus.OK : HttpStatus.CONFLICT).body(response);
	}

	@Operation(summary = "변경할 닉네임/캐릭터 사진 등록")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
			@ApiResponse(responseCode = "409", description = "점유 정보가 없거나 만료되었거나 본인이 아닙니다."),
			@ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임입니다.")
	})
	@PatchMapping("/profile")
	public ResponseEntity<NicknameChangeResponse> changeNickname(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody UpdateProfileRequest updateProfileRequest) {
		UpdateProfileCommand command = new UpdateProfileCommand(
				userDetails.getId(),
				updateProfileRequest.newNickname(),
				updateProfileRequest.characterId());
		UpdateProfileResult result = updateProfileUseCase.updateProfile(command);
		NicknameChangeResponse response = NicknameChangeResponse.from(result);
		return result.success()
				? ResponseEntity.ok(response)
				: ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@PutMapping("/profile-image")
	public ResponseEntity<Void> updateProfileImage(
			@AuthenticationPrincipal CustomUserDetails customUserDetails,
			@RequestParam Long imageId) {
		boolean success = updateProfileUseCase.updateProfileImage(customUserDetails.getId(), imageId);
		if (success) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
