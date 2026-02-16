package store.piku.back.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.piku.back.auth.dto.request.EmailValidRequest;
import store.piku.back.auth.dto.request.PwdResetRequest;
import store.piku.back.auth.dto.request.SignupRequest;
import store.piku.back.auth.service.AuthService;
import store.piku.back.auth.service.EmailService;

import java.util.List;
import java.util.Map;

@Tag(name = "Auth", description = "회원가입/이메일 인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    /*
     * 회원가입
     */
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
        authService.verifyCode(dto);

        return ResponseEntity.ok("이메일 인증이 성공적으로 완료되었습니다.");
    }

    @Operation(summary = "비밀번호 재설정", description = "비밀번호를 새로운 비밀번호로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 변경 실패")
    })
    @PostMapping("/password-reset")
    public ResponseEntity<String> verifyCodeAndResetPwd(@RequestBody PwdResetRequest dto) {
        authService.verifyCodeAndResetPwd(dto);

        return ResponseEntity.ok("이메일 인증 및 비밀번호 변경이 성공적으로 완료되었습니다.");
    }

    @GetMapping("/email")
    public ResponseEntity<String> checkEmailDomain(@RequestParam String email) {
        boolean isAllowed = emailService.isEmailAllowed(email);
        if (isAllowed) {
            return ResponseEntity.ok("허용된 이메일 도메인입니다.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("허용되지 않은 이메일 도메인입니다.");
        }
    }

    @Operation(summary = "허용된 이메일 도메인 목록 조회", description = "허용된 이메일 도메인 목록을 조회합니다.")
    @GetMapping("/email-domains")
    public ResponseEntity<List<String>> getAllowedEmailDomains() {
        List<String> response = emailService.getAllowedEmailDomains();
        return ResponseEntity.ok(response);
    }
}