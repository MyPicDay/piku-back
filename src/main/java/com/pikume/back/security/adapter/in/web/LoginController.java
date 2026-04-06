package com.pikume.back.security.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pikume.back.user.auth.constants.AuthConstants;
import com.pikume.back.global.dto.CookieSpec;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.dto.request.LoginRequest;
import com.pikume.back.security.dto.response.LoginResponse;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.util.CookieUtils;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;

@Tag(name = "Login", description = "로그인/로그아웃/토큰 재발급 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

	private final LoginUseCase loginUseCase;
	private final ReissueTokenUseCase reissueTokenUseCase;
	private final CookieUtils cookieUtils;
	private final ProblemDetailFactory problemDetailFactory;

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
			LoginResult loginResult = loginUseCase.login(dto, deviceId);
			log.info("[로그인] 성공 : 이메일={}", dto.getEmail());

			ResponseCookie responseCookie = toResponseCookie(
					loginUseCase.newCookieRefreshToken(loginResult.tokens().getRefreshToken()));

			LoginResponse loginResponse = new LoginResponse("로그인 성공", loginResult.userInfo());

			return ResponseEntity.ok()
					.header(HttpHeaders.AUTHORIZATION,
							AuthConstants.BEARER_PREFIX + loginResult.tokens().getAccessToken())
					.header(HttpHeaders.SET_COOKIE, responseCookie.toString())
					.body(loginResponse);
		} catch (InvalidCredentialsException e) {
			log.warn("[로그인] 실패 : {}", e.getMessage());
			return buildProblem(SecurityProblemType.INVALID_CREDENTIALS, e.getMessage(), request);
		}
	}

	@Operation(summary = "Access Token 재발급", description = "Cookie에 담긴 Refresh Token을 사용하여 새로운 Access Token을 재발급합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
			@ApiResponse(responseCode = "401", description = "Refresh Token 만료")
	})
	@PostMapping("/reissue")
	public ResponseEntity<?> reissue(HttpServletRequest request) {
		String refreshToken = cookieUtils.getCookieValue(request, AuthConstants.REFRESH_TOKEN);

		String newAccessToken = reissueTokenUseCase.reissueAccessToken(refreshToken);
		if (newAccessToken == null) {
			ResponseCookie resetCookie = toResponseCookie(loginUseCase.removeCookieRefreshToken());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.header(HttpHeaders.SET_COOKIE, resetCookie.toString())
					.body(problemDetailFactory.create(
							SecurityProblemType.INVALID_REFRESH_TOKEN,
							"유효하지 않은 Refresh Token입니다.",
							request.getRequestURI()));
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + newAccessToken)
				.body(new MessageResponse("토큰 재발급 성공"));
	}

	@Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고 Refresh Token을 삭제합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
			@ApiResponse(responseCode = "401", description = "로그인 상태가 아님")
	})
	@PostMapping("/logout")
	public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails user, HttpServletRequest request) {
		if (user == null || user.getEmail() == null) {
			return buildProblem(SecurityProblemType.UNAUTHENTICATED, "로그인 상태가 아닙니다.", request);
		}
		String deviceId = request.getHeader(AuthConstants.DEVICE_ID_HEADER);
		loginUseCase.logout(user.getEmail(), deviceId);

		ResponseCookie deleteCookie = toResponseCookie(loginUseCase.removeCookieRefreshToken());

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
				.body(new MessageResponse("로그아웃 완료"));
	}

	private ResponseEntity<ProblemDetail> buildProblem(SecurityProblemType problemType, String detail,
			HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, detail, request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}

	private ResponseCookie toResponseCookie(CookieSpec cookieSpec) {
		return ResponseCookie.from(cookieSpec.name(), cookieSpec.value())
				.httpOnly(cookieSpec.httpOnly())
				.secure(cookieSpec.secure())
				.path(cookieSpec.path())
				.maxAge(cookieSpec.maxAgeSeconds())
				.sameSite(cookieSpec.sameSite())
				.build();
	}
}
