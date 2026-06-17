package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.adapter.in.web.dto.request.SetAdminLoginIdRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.SetAdminPasswordRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.TemporaryAdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.VerifyAdminOtpRequest;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.service.AdminOnboardingStep;
import com.pikume.back.admin.application.service.AdminOtpRegistrationResult;
import com.pikume.back.admin.application.service.AdminTemporaryLoginResult;
import com.pikume.back.admin.application.service.CompleteAdminOnboardingResult;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.jwt.SecurityTokenType;
import com.pikume.back.user.auth.constants.AuthConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Onboarding", description = "관리자 최초 로그인과 온보딩 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminOnboardingController {

	private final AdminOnboardingUseCase adminOnboardingUseCase;
	private final JwtProvider jwtProvider;

	@Operation(summary = "관리자 임시 로그인", description = "이메일과 임시 패스워드로 최초 설정 전용 토큰을 발급합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "임시 로그인 성공"),
			@ApiResponse(responseCode = "401", description = "임시 로그인 실패 또는 만료")
	})
	@PostMapping("/temporary-login")
	public ResponseEntity<AdminTemporaryLoginResult> temporaryLogin(@RequestBody TemporaryAdminLoginRequest request) {
		return ResponseEntity.ok(adminOnboardingUseCase.temporaryLogin(request.email(), request.temporaryPassword()));
	}

	@Operation(summary = "정식 로그인 아이디 설정", description = "온보딩 토큰으로 정식 로그인 아이디를 설정합니다.")
	@PatchMapping("/onboarding/login-id")
	public ResponseEntity<OnboardingNextStepResponse> setLoginId(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody SetAdminLoginIdRequest request) {
		adminOnboardingUseCase.setLoginId(resolveOnboardingAdminId(authorization), request.loginId());
		return ResponseEntity.ok(new OnboardingNextStepResponse(AdminOnboardingStep.SET_PASSWORD.name()));
	}

	@Operation(summary = "정식 패스워드 설정", description = "온보딩 토큰으로 정식 패스워드를 설정하고 임시 패스워드를 무효화합니다.")
	@PatchMapping("/onboarding/password")
	public ResponseEntity<OnboardingNextStepResponse> setPassword(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody SetAdminPasswordRequest request) {
		adminOnboardingUseCase.setPassword(resolveOnboardingAdminId(authorization), request.password());
		return ResponseEntity.ok(new OnboardingNextStepResponse(AdminOnboardingStep.REGISTER_OTP.name()));
	}

	@Operation(summary = "OTP 등록 정보 발급", description = "온보딩 토큰으로 OTP 앱 등록 정보를 발급합니다.")
	@PostMapping("/onboarding/otp")
	public ResponseEntity<AdminOtpRegistrationResult> startOtpRegistration(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return ResponseEntity.ok(adminOnboardingUseCase.startOtpRegistration(resolveOnboardingAdminId(authorization)));
	}

	@Operation(summary = "최초 OTP 인증", description = "OTP 코드를 검증하고 관리자 Access Token을 발급합니다.")
	@PostMapping("/onboarding/otp/verify")
	public ResponseEntity<CompleteAdminOnboardingResult> verifyOtp(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody VerifyAdminOtpRequest request) {
		return ResponseEntity.ok(adminOnboardingUseCase.verifyOtp(resolveOnboardingAdminId(authorization), request.otpCode()));
	}

	private String resolveOnboardingAdminId(String authorization) {
		if (authorization == null || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
			throw new AdminException(AdminProblem.ONBOARDING_TOKEN_INVALID, "온보딩 토큰이 필요합니다.");
		}
		String token = authorization.substring(AuthConstants.BEARER_PREFIX.length());
		if (!jwtProvider.validateToken(token) || jwtProvider.getTokenType(token) != SecurityTokenType.ADMIN_ONBOARDING) {
			throw new AdminException(AdminProblem.ONBOARDING_TOKEN_INVALID, "온보딩 토큰이 유효하지 않습니다.");
		}
		return jwtProvider.getUserIdFromToken(token);
	}

	private record OnboardingNextStepResponse(String nextStep) {
	}
}
