package store.piku.back.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import store.piku.back.auth.dto.LoginRequest;
import store.piku.back.auth.dto.SignupRequest;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.dto.response.LoginResponse;
import store.piku.back.auth.entity.RefreshToken;
import store.piku.back.auth.jwt.JwtProvider;
import store.piku.back.auth.repository.RefreshTokenRepository;
import store.piku.back.auth.service.AuthService;
import store.piku.back.global.config.CustomUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.global.util.CookieUtils;

import java.util.concurrent.TimeUnit;

@Tag(name = "Auth", description = "인증/인가 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieUtils cookieUtils;

    /*
    * 회원가입
    * */
    @Operation(summary = "회원가입", description = "사용자 정보를 받아 회원가입을 진행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "회원가입 실패")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest dto) {
        log.info("[회원가입] 요청 : 이메일={}, 닉네임={}", dto.getEmail(), dto.getNickname());
        try {
            authService.signup(dto);
            log.info("[회원가입] 성공 : 이메일={}", dto.getEmail());
            return ResponseEntity.ok("회원가입 성공");
        } catch (RuntimeException e) {
            log.warn("[회원가입] 실패 : {}", e.getMessage());
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }

    /*
    * 로그인
    * */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인을 진행하고 Access/Refresh 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest dto, HttpServletRequest request) {
        String deviceId = request.getHeader("Device-Id");
        log.info("[로그인] 요청 수신: 이메일={}", dto.getEmail());

        try {
            TokenDto tokens = authService.login(dto, deviceId);
            UserInfo userInfo = authService.getUserInfoByEmail(dto.getEmail());
            log.info("[로그인] 성공 : 이메일={}", dto.getEmail());

            ResponseCookie responseCookie = ResponseCookie.from("refreshToken",tokens.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(TimeUnit.DAYS.toSeconds(7))
                    .sameSite("Lax")
                    .build();

            LoginResponse loginResponse = new LoginResponse("로그인 성공", userInfo);

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken())
                    .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                    .body(loginResponse);
        } catch (RuntimeException e) {
            log.warn("[로그인] 실패 : {}", e.getMessage());
            return ResponseEntity.status(401).body("로그인 실패: " + e.getMessage());
        }
    }

    /*
     * access token 재발급
     * */
    @Operation(summary = "Access Token 재발급", description = "Cookie에 담긴 Refresh Token을 사용하여 새로운 Access Token을 재발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 만료")
    })
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request) {
        String refreshToken = cookieUtils.getCookieValue(request, "refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token이 없습니다.");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            refreshTokenRepository.deleteByRefreshToken(refreshToken);
            return ResponseEntity.status(401).body("Refresh Token 만료됨");
        }

        // 저장된 refreshToken에서 email 추출
        RefreshToken tokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("저장된 리프레시 토큰 없음"));

        String email = tokenEntity.getKey().split("-")[0];

        // 새 Access Token 발급
        String newAccessToken = jwtProvider.generateAccessToken(email);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken)
                .body("토큰 재발급 성공");
    }

    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고 Refresh Token을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 상태가 아님")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails user, HttpServletRequest request) {
        if (user == null || user.getEmail() == null) {
            return ResponseEntity.status(401).body("로그인 상태가 아닙니다.");
        }
        String key = user.getEmail() + "-" + request.getHeader("Device-Id");
        refreshTokenRepository.deleteById(key);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("로그아웃 완료");

    }

}

