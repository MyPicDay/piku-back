package com.pikume.back.security.adapter.in.web;

import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.dto.ReissueResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;
import com.pikume.back.security.dto.request.LoginRequest;
import com.pikume.back.security.dto.request.MobileLogoutRequest;
import com.pikume.back.security.dto.request.MobileReissueRequest;
import com.pikume.back.security.dto.response.MobileLoginResponse;
import com.pikume.back.security.dto.response.MobileReissueResponse;
import com.pikume.back.security.dto.response.MobileTokenBundle;
import com.pikume.back.user.auth.constants.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

	private final LoginUseCase loginUseCase;
	private final ReissueTokenUseCase reissueTokenUseCase;
	private final ProblemDetailFactory problemDetailFactory;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest dto, HttpServletRequest request) {
		String deviceId = request.getHeader(AuthConstants.DEVICE_ID_HEADER);

		try {
			LoginResult loginResult = loginUseCase.login(dto, deviceId);
			return ResponseEntity.ok(new MobileLoginResponse(
					"로그인 성공",
					loginResult.userInfo(),
					new MobileTokenBundle(
							"Bearer",
							loginResult.tokens().getAccessToken(),
							loginResult.tokens().getRefreshToken(),
							AuthConstants.ACCESS_TOKEN_EXPIRATION_TIME / 1000L,
							AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME / 1000L)));
		} catch (InvalidCredentialsException e) {
			return buildProblem(SecurityProblemType.INVALID_CREDENTIALS, e.getMessage(), request);
		}
	}

	@PostMapping("/reissue")
	public ResponseEntity<?> reissue(@RequestBody MobileReissueRequest dto, HttpServletRequest request) {
		ReissueResult result = reissueTokenUseCase.reissueTokens(dto.refreshToken());
		if (result == null) {
			return buildProblem(SecurityProblemType.INVALID_REFRESH_TOKEN, "유효하지 않은 Refresh Token입니다.", request);
		}

		return ResponseEntity.ok(new MobileReissueResponse(
				"토큰 재발급 성공",
				new MobileTokenBundle(
						"Bearer",
						result.accessToken(),
						result.refreshToken(),
						result.accessTokenExpiresIn(),
						result.refreshTokenExpiresIn())));
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestBody MobileLogoutRequest dto, HttpServletRequest request) {
		if (!StringUtils.hasText(dto.refreshToken())) {
			return buildProblem(SecurityProblemType.INVALID_REFRESH_TOKEN, "유효하지 않은 Refresh Token입니다.", request);
		}

		loginUseCase.logoutByRefreshToken(dto.refreshToken());
		return ResponseEntity.ok(new MessageResponse("로그아웃 완료"));
	}

	private ResponseEntity<ProblemDetail> buildProblem(SecurityProblemType problemType, String detail,
			HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, detail, request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
