package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.adapter.in.web.dto.request.AdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.ChangeAdminPasswordRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.VerifyAdminOtpRequest;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
import com.pikume.back.admin.application.service.AdminLoginChallengeResult;
import com.pikume.back.admin.application.service.AdminTokenIssueResult;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.security.config.AdminUserDetails;
import com.pikume.back.security.jwt.AdminAuthConstants;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.jwt.SecurityTokenType;
import com.pikume.back.user.auth.constants.AuthConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Auth", description = "관리자 정식 로그인, 토큰 재발급, 로그아웃 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminAuthUseCase adminAuthUseCase;
	private final JwtProvider jwtProvider;

	@Operation(summary = "관리자 정식 로그인", description = "정식 로그인 아이디와 패스워드로 OTP 인증 전용 토큰을 발급합니다.")
	@PostMapping("/login")
	public ResponseEntity<AdminLoginChallengeResult> login(@RequestBody AdminLoginRequest request) {
		return ResponseEntity.ok(adminAuthUseCase.login(request.loginId(), request.password()));
	}

	@Operation(summary = "관리자 OTP 인증", description = "OTP challenge token과 OTP 코드로 관리자 Access/Refresh Token을 발급합니다.")
	@PostMapping("/otp/verify")
	public ResponseEntity<AdminTokenResponse> verifyOtp(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody VerifyAdminOtpRequest request) {
		AdminTokenIssueResult result = adminAuthUseCase.verifyOtp(resolveOtpChallengeAdminId(authorization), request.otpCode());
		return tokenResponse(result);
	}

	@Operation(summary = "관리자 토큰 재발급", description = "관리자 Refresh Token 쿠키를 회전하고 새 Access Token을 발급합니다.")
	@PostMapping("/reissue")
	public ResponseEntity<AdminTokenResponse> reissue(
			@CookieValue(value = AdminAuthConstants.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
		return tokenResponse(adminAuthUseCase.reissue(refreshToken));
	}

	@Operation(summary = "관리자 로그아웃", description = "현재 관리자 세션을 폐기하고 Refresh Token 쿠키를 제거합니다.")
	@PostMapping("/logout")
	public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal AdminUserDetails admin) {
		if (admin == null) {
			throw new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		adminAuthUseCase.logout(admin.getId(), admin.getSessionId());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, removeRefreshTokenCookie().toString())
				.body(new MessageResponse("로그아웃 완료"));
	}

	@Operation(summary = "관리자 패스워드 변경", description = "현재 패스워드를 검증한 뒤 새 패스워드로 변경합니다.")
	@PatchMapping("/password")
	public ResponseEntity<MessageResponse> changePassword(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestBody ChangeAdminPasswordRequest request) {
		if (admin == null) {
			throw new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		adminAuthUseCase.changePassword(admin.getId(), request.currentPassword(), request.newPassword());
		return ResponseEntity.ok(new MessageResponse("패스워드 변경 완료"));
	}

	private ResponseEntity<AdminTokenResponse> tokenResponse(AdminTokenIssueResult result) {
		return ResponseEntity.ok()
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + result.accessToken())
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result).toString())
				.body(AdminTokenResponse.from(result));
	}

	private String resolveOtpChallengeAdminId(String authorization) {
		if (authorization == null || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
			throw new AdminException(AdminProblem.OTP_CHALLENGE_TOKEN_INVALID, "OTP challenge token이 필요합니다.");
		}
		String token = authorization.substring(AuthConstants.BEARER_PREFIX.length());
		if (!jwtProvider.validateToken(token) || jwtProvider.getTokenType(token) != SecurityTokenType.ADMIN_OTP_CHALLENGE) {
			throw new AdminException(AdminProblem.OTP_CHALLENGE_TOKEN_INVALID, "OTP challenge token이 유효하지 않습니다.");
		}
		return jwtProvider.getUserIdFromToken(token);
	}

	private ResponseCookie refreshTokenCookie(AdminTokenIssueResult result) {
		return ResponseCookie.from(AdminAuthConstants.REFRESH_TOKEN_COOKIE_NAME, result.refreshToken())
				.httpOnly(true)
				.secure(true)
				.path("/api/admin/auth")
				.maxAge(result.refreshTokenExpiresInSeconds())
				.sameSite("Lax")
				.build();
	}

	private ResponseCookie removeRefreshTokenCookie() {
		return ResponseCookie.from(AdminAuthConstants.REFRESH_TOKEN_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(true)
				.path("/api/admin/auth")
				.maxAge(0)
				.sameSite("Lax")
				.build();
	}
}
