package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.adapter.in.web.dto.request.AdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.ChangeAdminPasswordRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.VerifyAdminOtpRequest;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
import com.pikume.back.admin.application.service.AdminAuthenticationResult;
import com.pikume.back.admin.application.service.AdminLoginChallengeResult;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.security.config.AdminSessionCookieManager;
import com.pikume.back.security.config.AdminUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Admin Auth", description = "관리자 세션 로그인과 로그아웃 API")
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "유효하지 않은 인증 요청",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "401", description = "관리자 인증 실패",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "Origin 또는 CSRF 검증 실패",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "423", description = "관리자 계정 잠금",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "429", description = "OTP 인증 일시 차단",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "503", description = "관리자 인증 저장소 확인 불가",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class)))
})
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminAuthUseCase adminAuthUseCase;
	private final AdminSessionCookieManager cookieManager;

	@Operation(summary = "관리자 정식 로그인", description = "사전 세션을 OTP 검증 단계로 전환합니다.")
	@PostMapping("/login")
	public ResponseEntity<AdminLoginChallengeResult> login(
			HttpServletRequest servletRequest, @RequestBody AdminLoginRequest request) {
		String sessionToken = cookieManager.requireSessionToken(servletRequest);
		return ResponseEntity.ok(adminAuthUseCase.login(sessionToken, request.loginId(), request.password()));
	}

	@Operation(summary = "관리자 OTP 인증", description = "OTP 성공 시 세션 및 CSRF 쿠키를 교체합니다.")
	@PostMapping("/otp/verify")
	public ResponseEntity<AdminAuthenticationResponse> verifyOtp(
			HttpServletRequest servletRequest, @RequestBody VerifyAdminOtpRequest request) {
		String sessionToken = cookieManager.requireSessionToken(servletRequest);
		AdminAuthenticationResult result = adminAuthUseCase.verifyOtp(sessionToken, request.otpCode());
		return authenticatedResponse(result);
	}

	@Operation(summary = "관리자 로그아웃", description = "현재 관리자 세션과 관련 쿠키를 폐기합니다.")
	@PostMapping("/logout")
	public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal AdminUserDetails admin) {
		if (admin == null) {
			throw new AdminException(AdminErrorCode.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		adminAuthUseCase.logout(admin.getId(), admin.getSessionId());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE,
						cookieManager.expiredSessionCookie().toString(),
						cookieManager.expiredCsrfCookie().toString())
				.body(new MessageResponse("로그아웃 완료"));
	}

    /**
	@Operation(summary = "관리자 패스워드 변경")
	@PatchMapping("/password")
	public ResponseEntity<MessageResponse> changePassword(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestBody ChangeAdminPasswordRequest request) {
		if (admin == null) {
			throw new AdminException(AdminErrorCode.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		adminAuthUseCase.changePassword(admin.getId(), request.currentPassword(), request.newPassword());
		return ResponseEntity.ok(new MessageResponse("패스워드 변경 완료"));
	}
     */

	private ResponseEntity<AdminAuthenticationResponse> authenticatedResponse(AdminAuthenticationResult result) {
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE,
						cookieManager.sessionCookie(result.credentials(), Duration.ofHours(8)).toString(),
						cookieManager.csrfCookie(result.credentials(), Duration.ofHours(8)).toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(AdminAuthenticationResponse.from(result));
	}
}
