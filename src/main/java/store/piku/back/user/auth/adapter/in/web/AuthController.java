package store.piku.back.user.auth.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.piku.back.user.auth.application.port.in.ResetPasswordUseCase;
import store.piku.back.user.auth.application.port.in.SignUpUseCase;
import store.piku.back.user.auth.application.port.in.VerifyEmailUseCase;
import store.piku.back.user.auth.application.port.out.SendVerificationEmailPort;
import store.piku.back.user.auth.dto.request.EmailValidRequest;
import store.piku.back.user.auth.dto.request.PwdResetRequest;
import store.piku.back.user.auth.dto.request.SignupRequest;

import java.util.List;
import java.util.Map;

@Tag(name = "Auth", description = "회원가입/이메일 인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final SignUpUseCase signUpUseCase;
	private final VerifyEmailUseCase verifyEmailUseCase;
	private final ResetPasswordUseCase resetPasswordUseCase;
	private final SendVerificationEmailPort sendVerificationEmailPort;

	@Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입을 진행합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "회원가입 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청")
	})
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignupRequest dto) {
		try {
			signUpUseCase.signup(dto);
			return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
		} catch (RuntimeException e) {
			log.warn("[회원가입] 실패 : {}", e.getMessage());
			return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
		}
	}

	@Operation(summary = "회원가입 이메일 발송", description = "회원가입시 사용자 본인인증과 이메일 중복확인을 위해 인증코드를 이메일로 발송합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "인증 이메일 발송 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청")
	})
	@PostMapping("/send-verification/sign-up")
	public ResponseEntity<?> sendSignUpVerificationEmail(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		try {
			verifyEmailUseCase.sendSignUpVerificationEmail(email);
			return ResponseEntity.ok("회원가입 인증 이메일이 발송되었습니다.");
		} catch (RuntimeException e) {
			log.warn("[이메일 발송] 실패 : {}", e.getMessage());
			return ResponseEntity.badRequest().body("이메일 발송 실패: " + e.getMessage());
		}
	}

	@Operation(summary = "비밀번호 재설정 이메일 발송", description = "비밀번호 재설정을 위한 인증코드를 이메일로 발송합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "인증 이메일 발송 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청")
	})
	@PostMapping("/send-verification/password-reset")
	public ResponseEntity<?> sendPasswordResetVerificationEmail(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		try {
			verifyEmailUseCase.sendPasswordResetVerificationEmail(email);
			return ResponseEntity.ok("비밀번호 재설정 인증 이메일이 발송되었습니다.");
		} catch (RuntimeException e) {
			log.warn("[이메일 발송] 실패: {}", e.getMessage());
			return ResponseEntity.badRequest().body("이메일 발송 실패: " + e.getMessage());
		}
	}

	@Operation(summary = "이메일 인증 코드 검증", description = "사용자가 입력한 인증 코드를 검증합니다.")
	@PostMapping("/verify-code")
	public ResponseEntity<?> verifyCode(@RequestBody EmailValidRequest dto) {
		try {
			verifyEmailUseCase.verifyCode(dto);
			return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
		} catch (RuntimeException e) {
			log.warn("[코드 검증] 실패 : {}", e.getMessage());
			return ResponseEntity.badRequest().body("코드 검증 실패: " + e.getMessage());
		}
	}

	@Operation(summary = "비밀번호 재설정", description = "인증 이메일을 통해 비밀번호를 재설정합니다.")
	@PostMapping("/password-reset")
	public ResponseEntity<?> resetPassword(@RequestBody PwdResetRequest dto) {
		try {
			resetPasswordUseCase.verifyCodeAndResetPwd(dto);
			return ResponseEntity.ok("비밀번호가 재설정되었습니다.");
		} catch (RuntimeException e) {
			log.warn("[비밀번호 재설정] 실패: {}", e.getMessage());
			return ResponseEntity.badRequest().body("비밀번호 재설정 실패: " + e.getMessage());
		}
	}

	@Operation(summary = "이메일 허용 여부 확인", description = "이메일이 허용된 도메인에 속하는지 확인합니다.")
	@GetMapping("/email")
	public ResponseEntity<?> isEmailAllowed(@RequestParam String email) {
		boolean allowed = sendVerificationEmailPort.isEmailAllowed(email);
		return ResponseEntity.ok(Map.of("allowed", allowed));
	}

	@Operation(summary = "허용된 이메일 도메인 목록 조회", description = "허용된 이메일 도메인 목록을 반환합니다.")
	@GetMapping("/email-domains")
	public ResponseEntity<List<String>> getAllowedEmailDomains() {
		return ResponseEntity.ok(sendVerificationEmailPort.getAllowedEmailDomains());
	}
}
