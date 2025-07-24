package store.piku.back.user.controller;

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
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;
import store.piku.back.user.dto.response.NicknameChangeResponseDTO;
import store.piku.back.user.dto.response.NicknameResponseDTO;
import store.piku.back.user.dto.response.ProfilePreviewDTO;
import store.piku.back.user.dto.response.UserProfileResponseDTO;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.UserProfileService;
import store.piku.back.user.service.UserService;
import store.piku.back.user.service.reader.UserReader;


@Tag(name = "Users", description = "유저 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileServie;
    private final RequestMetaMapper requestMetaMapper;
    private final UserService userService;
    private final UserReader userReader;

    @Operation(summary = "프로필 미리보기 정보 반환", description = "사용자의 프로필 미리보 시 사용될 정보를 조회하여 반환합니다.")
    @GetMapping("/{userId}/profile-preview")
    public ResponseEntity<ProfilePreviewDTO> getProfilePreview(@PathVariable String userId, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        log.info("사용자 {}의 프로필 미리보기 조회 요청", userId);

        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        ProfilePreviewDTO profilePreview = userProfileServie.getProfilePreviewByUserId(userId, userDetails.getId(), requestMetaInfo);
        return ResponseEntity.status(HttpStatus.OK).body(profilePreview);
    }
    @Operation(summary = "사용자 프로필 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable String userId, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        UserProfileResponseDTO profile = userProfileServie.getUserProfile(userId,userDetails.getId(),requestMetaInfo);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "닉네임 중복조회 검사"
    ,  responses = {
            @ApiResponse(responseCode = "200", description = "사용 가능한 닉네임입니다."),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임입니다.")
    }
    )
    @GetMapping("/nickname/availability")
    public ResponseEntity<NicknameResponseDTO> checkNickname(@RequestParam String nickname, @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean reserved = userService.tryReserveNickname(nickname, userDetails.getId());
        NicknameResponseDTO dto = new NicknameResponseDTO(
                reserved,
                reserved ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다."
        );
        return ResponseEntity.status(reserved ? HttpStatus.OK : HttpStatus.CONFLICT).body(dto);
    }



    @Operation(summary = "변경할 닉네임 등록")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
            @ApiResponse(responseCode = "409", description = "점유 정보가 없거나 만료되었거나 본인이 아닙니다."),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임입니다.")
    })
    @PatchMapping("/nickname")
    public ResponseEntity<NicknameChangeResponseDTO> changeNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                    @RequestParam String newNickname) {
        NicknameChangeResponseDTO changed = userService.reserveAndChangeNickname(userDetails.getId(), newNickname);
        return changed.isSuccess()
                ? ResponseEntity.ok(changed)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(changed);
    }


    @PutMapping("/profile-image")
    public ResponseEntity<Void> updateProfileImage(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestParam Long imageId) {

        boolean success = userService.updateProfileImage(customUserDetails.getId(), imageId);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
