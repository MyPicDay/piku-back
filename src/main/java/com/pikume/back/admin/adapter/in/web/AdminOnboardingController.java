package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.adapter.in.web.dto.request.SetAdminCredentialsRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.TemporaryAdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.VerifyAdminOtpRequest;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.service.AdminAuthenticationResult;
import com.pikume.back.admin.application.service.AdminOnboardingStep;
import com.pikume.back.admin.application.service.AdminOtpRegistrationResult;
import com.pikume.back.admin.application.service.AdminTemporaryLoginResult;
import com.pikume.back.security.config.AdminSessionCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Admin Onboarding", description = "관리자 최초 로그인과 온보딩 API")
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "유효하지 않은 온보딩 요청",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "401", description = "임시 자격 증명 또는 OTP 인증 실패",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "Origin 또는 CSRF 검증 실패",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "409", description = "관리자 로그인 아이디 중복",
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
@ConditionalOnProperty(prefix = "admin.api", name = "onboarding-enabled", havingValue = "true")
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminOnboardingController {

	private final AdminOnboardingUseCase adminOnboardingUseCase;
	private final AdminSessionCookieManager cookieManager;

	@Operation(summary = "관리자 임시 로그인", description = "사전 세션을 최초 설정 단계에 결합합니다.")
	@PostMapping("/temporary-login")
	public ResponseEntity<AdminTemporaryLoginResult> temporaryLogin(
			HttpServletRequest servletRequest, @RequestBody TemporaryAdminLoginRequest request) {
		return ResponseEntity.ok(adminOnboardingUseCase.temporaryLogin(
				cookieManager.requireSessionToken(servletRequest), request.email(), request.temporaryPassword()));
	}

	@PatchMapping("/onboarding/credentials")
	public ResponseEntity<OnboardingNextStepResponse> setCredentials(
			HttpServletRequest servletRequest, @RequestBody SetAdminCredentialsRequest request) {
		adminOnboardingUseCase.setCredentials(
				cookieManager.requireSessionToken(servletRequest), request.loginId(), request.password());
		return ResponseEntity.ok(new OnboardingNextStepResponse(AdminOnboardingStep.REGISTER_OTP.name()));
	}

	@PostMapping("/onboarding/otp")
	public ResponseEntity<AdminOtpRegistrationResult> startOtpRegistration(HttpServletRequest servletRequest) {
		return ResponseEntity.ok(adminOnboardingUseCase.startOtpRegistration(
				cookieManager.requireSessionToken(servletRequest)));
	}

	@PostMapping("/onboarding/otp/verify")
	public ResponseEntity<AdminAuthenticationResponse> verifyOtp(
			HttpServletRequest servletRequest, @RequestBody VerifyAdminOtpRequest request) {
		String sessionToken = cookieManager.requireSessionToken(servletRequest);
		AdminAuthenticationResult result = adminOnboardingUseCase.verifyOtp(sessionToken, request.otpCode());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE,
						cookieManager.sessionCookie(result.credentials(), Duration.ofHours(8)).toString(),
						cookieManager.csrfCookie(result.credentials(), Duration.ofHours(8)).toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(AdminAuthenticationResponse.from(result));
	}

	private record OnboardingNextStepResponse(String nextStep) {
	}
}
