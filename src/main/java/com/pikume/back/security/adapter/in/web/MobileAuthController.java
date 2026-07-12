package com.pikume.back.security.adapter.in.web;

import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.user.auth.application.dto.LoginCommand;
import com.pikume.back.user.auth.application.dto.LoginResult;
import com.pikume.back.user.auth.application.dto.ReissueSessionResult;
import com.pikume.back.user.auth.application.exception.InvalidCredentialsException;
import com.pikume.back.user.auth.application.port.in.LoginUseCase;
import com.pikume.back.user.auth.application.port.in.LogoutUseCase;
import com.pikume.back.user.auth.application.port.in.ReissueSessionUseCase;
import com.pikume.back.security.adapter.in.web.dto.request.LoginRequest;
import com.pikume.back.security.adapter.in.web.dto.request.MobileLogoutRequest;
import com.pikume.back.security.adapter.in.web.dto.request.MobileReissueRequest;
import com.pikume.back.security.adapter.in.web.dto.response.MobileLoginResponse;
import com.pikume.back.security.adapter.in.web.dto.response.MobileReissueResponse;
import com.pikume.back.security.adapter.in.web.dto.response.MobileTokenBundle;
import com.pikume.back.security.config.UserTokenSettings;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

	private final LoginUseCase loginUseCase;
	private final ReissueSessionUseCase reissueSessionUseCase;
	private final LogoutUseCase logoutUseCase;
	private final ProblemDetailFactory problemDetailFactory;
	private final AuthUserResponseMapper authUserResponseMapper;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest dto, HttpServletRequest request) {
		String deviceId = request.getHeader(AuthWebConstants.DEVICE_ID_HEADER);

		try {
			LoginResult loginResult = loginUseCase.login(new LoginCommand(dto.getEmail(), dto.getPassword(), deviceId));
			return ResponseEntity.ok(new MobileLoginResponse(
					"로그인 성공",
					authUserResponseMapper.toDisplayUserInfo(loginResult.userInfo()),
					new MobileTokenBundle(
							"Bearer",
							loginResult.accessToken(),
							loginResult.refreshToken(),
							UserTokenSettings.ACCESS_TOKEN_EXPIRATION_MILLIS / 1000L,
							UserTokenSettings.REFRESH_TOKEN_EXPIRATION_MILLIS / 1000L)));
		} catch (InvalidCredentialsException e) {
			return buildProblem(SecurityProblemType.INVALID_CREDENTIALS, e.getMessage(), request);
		}
	}

	@PostMapping("/reissue")
	public ResponseEntity<?> reissue(@RequestBody MobileReissueRequest dto, HttpServletRequest request) {
		ReissueSessionResult result = reissueSessionUseCase.reissueSession(dto.refreshToken());
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
		String deviceId = request.getHeader(AuthWebConstants.DEVICE_ID_HEADER);
		logoutUseCase.logoutByRefreshToken(dto.refreshToken(), deviceId);
		return ResponseEntity.ok(new MessageResponse("로그아웃 완료"));
	}

	private ResponseEntity<ProblemDetail> buildProblem(SecurityProblemType problemType, String detail,
			HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, detail, request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
