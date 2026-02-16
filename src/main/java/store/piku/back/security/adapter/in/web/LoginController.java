package store.piku.back.security.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.dto.request.LoginRequest;
import store.piku.back.auth.dto.response.LoginResponse;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.util.CookieUtils;
import store.piku.back.security.application.port.in.LoginUseCase;
import store.piku.back.security.application.port.in.ReissueTokenUseCase;

@Tag(name = "Login", description = "로그인/로그아웃/토큰 재발급 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

	private final LoginUseCase loginUseCase;
	private final ReissueTokenUseCase reissueTokenUseCase;
	private final CookieUtils cookieUtils;

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
			TokenDto tokens = loginUseCase.login(dto, deviceId);
			UserInfo userInfo = loginUseCase.getUserInfoByEmail(dto.getEmail());
			log.info("[로그인] 성공 : 이메일={}", dto.getEmail());

			ResponseCookie responseCookie = loginUseCase.newCookieRefreshToken(tokens.getRefreshToken());

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

	@Operation(summary = "Access Token 재발급", description = "Cookie에 담긴 Refresh Token을 사용하여 새로운 Access Token을 재발급합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
			@ApiResponse(responseCode = "401", description = "Refresh Token 만료")
	})
	@PostMapping("/reissue")
	public ResponseEntity<?> reissue(HttpServletRequest request) {
		String refreshToken = cookieUtils.getCookieValue(request, AuthConstants.REFRESH_TOKEN);

		String newAccessToken = reissueTokenUseCase.reissueAccessToken(refreshToken);
		ResponseCookie resetCookie = loginUseCase.removeCookieRefreshToken();
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
		String deviceId = request.getHeader(AuthConstants.DEVICE_ID_HEADER);
		loginUseCase.logout(user.getEmail(), deviceId);

		ResponseCookie deleteCookie = loginUseCase.removeCookieRefreshToken();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
				.body("로그아웃 완료");
	}
}
