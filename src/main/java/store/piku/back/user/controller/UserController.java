package store.piku.back.user.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import store.piku.back.user.dto.response.ProfilePreviewDTO;
import store.piku.back.user.service.UserProfileService;


@Tag(name = "Users", description = "유저 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileServie;
    private final RequestMetaMapper requestMetaMapper;

    @Operation(summary = "프로필 미리보기 정보 반환", description = "사용자의 프로필 미리보 시 사용될 정보를 조회하여 반환합니다.")
    @GetMapping("/{userId}/profile-preview")
    public ResponseEntity<ProfilePreviewDTO> getProfilePreview(@PathVariable String userId, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        log.info("사용자 {}의 프로필 미리보기 조회 요청", userId);

        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        ProfilePreviewDTO profilePreview = userProfileServie.getProfilePreviewByUserId(userId, userDetails.getId(), requestMetaInfo);
        return ResponseEntity.status(HttpStatus.OK).body(profilePreview);
    }

}
