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
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.auth.dto.request.EmailValidRequest;
import store.piku.back.auth.dto.request.LoginRequest;
import store.piku.back.auth.dto.request.PwdResetRequest;
import store.piku.back.auth.dto.request.SignupRequest;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.dto.response.LoginResponse;
import store.piku.back.auth.enums.VerificationType;
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
import java.util.Map;

@Tag(name = "Auth", description = "인증/인가 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
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
        String deviceId = request.getHeader(AuthConstants.DEVICE_ID_HEADER);
        log.info("[로그인] 요청 수신: 이메일={}", dto.getEmail());

        try {
            TokenDto tokens = authService.login(dto, deviceId);
            UserInfo userInfo = authService.getUserInfoByEmail(dto.getEmail());
            log.info("[로그인] 성공 : 이메일={}", dto.getEmail());

            ResponseCookie responseCookie = authService.newCookieRefreshToken(tokens.getRefreshToken());

            LoginResponse loginResponse = new LoginResponse("로그인 성공", userInfo);

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + tokens.getAccessToken())
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
        String refreshToken = cookieUtils.getCookieValue(request, AuthConstants.REFRESH_TOKEN);

        String newAccessToken = authService.reissueAccessToken(refreshToken);
        ResponseCookie resetCookie = authService.removeCookieRefreshToken();
        if (newAccessToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, resetCookie.toString())
                    .body("Access Token 재발급 실패: 유효하지 않은 Refresh Token");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + newAccessToken)
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
        String key = user.getEmail() + "-" + request.getHeader(AuthConstants.DEVICE_ID_HEADER);
        refreshTokenRepository.deleteById(key);

        ResponseCookie deleteCookie = authService.removeCookieRefreshToken();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("로그아웃 완료");

    }



    @Operation(summary = "회원가입 이메일 발송", description = "회원가입시 사용자 본인인증과 이메일 중복확인을 위해 인증코드를 이메일로 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 발송 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 발송 실패")
    })
    @PostMapping("/send-verification/sign-up")
    public ResponseEntity<String> sendSignUpVerificationEmail(@RequestBody Map<String, String> request) {
        authService.sendSignUpVerificationEmail(request.get("email"));
        return ResponseEntity.ok("회원가입용 인증 이메일이 발송되었습니다.");
    }

    @Operation(summary = "비밀번호 재설정 이메일 발송", description = "비밀번호 재설정시 사용자 본인인증을 위해 인증코드를 이메일로 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 발송 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 발송 실패")
    })
    @PostMapping("/send-verification/password-reset")
    public ResponseEntity<String> sendPasswordResetVerificationEmail(@RequestBody Map<String, String> request) {
        authService.sendPasswordResetVerificationEmail(request.get("email"));
        return ResponseEntity.ok("비밀번호 재설정용 인증 이메일이 발송되었습니다.");
    }


    @Operation(summary = "이메일 인증 코드 검증", description = "사용자 본인인증을 위해 발송된 인증코드가 유효하고, 일치하는지 검증합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 코드 검증 성공"),
            @ApiResponse(responseCode = "401", description = "인증 코드 검증 실패")
    })
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody EmailValidRequest dto) {
        authService.verifyCode(dto, VerificationType.SIGN_UP);

        return ResponseEntity.ok("이메일 인증이 성공적으로 완료되었습니다.");
    }


    @Operation(summary = "이메일 인증 및 비밀번호 재설정", description = "본인인증을 위해 발송된 인증코드가 유효하고, 일치하는지 검증 후 새로운 비밀번호로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 코드 검증 및 비밀번호 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 코드 검증 및 비밀번호 변경 실패")
    })
    @PostMapping("/verify-code/password-reset")
    public ResponseEntity<String> verifyCodeAndResetPwd(@RequestBody PwdResetRequest dto) {
        authService.verifyCodeAndResetPwd(dto);

        return ResponseEntity.ok("이메일 인증 및 비밀번호 변경이 성공적으로 완료되었습니다.");
    }
}