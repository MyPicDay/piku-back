package com.pikume.back.security.adapter.in.web;

import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.security.application.dto.AuthenticatedUserInfo;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.dto.ReissueResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.UserInfo;
import com.pikume.back.security.dto.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MobileAuthController")
class MobileAuthControllerTest {

	@InjectMocks
	private MobileAuthController mobileAuthController;

	@Mock
	private LoginUseCase loginUseCase;

	@Mock
	private ReissueTokenUseCase reissueTokenUseCase;

	@Mock
	private AuthUserResponseMapper authUserResponseMapper;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mobileAuthController = new MobileAuthController(
				loginUseCase,
				reissueTokenUseCase,
				new ProblemDetailFactory(),
				authUserResponseMapper);
		mockMvc = MockMvcBuilders.standaloneSetup(mobileAuthController)
				.setControllerAdvice(new GlobalExceptionHandler(Optional.empty(), new ProblemDetailFactory()))
				.build();
	}

	@Test
	@DisplayName("POST /api/mobile/auth/login은 성공 시 user와 tokens를 body로 반환한다")
	void loginReturnsBodyTokensWhenSuccessful() throws Exception {
		LoginResult result = new LoginResult(
				new TokenDto("access-token", "refresh-token"),
				new AuthenticatedUserInfo(
						"user-1",
						"pikume",
						"public/characters/fixed/base_image_1.webp"));
		UserInfo displayUserInfo = new UserInfo(
				"user-1",
				"pikume",
				"https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
		given(loginUseCase.login(any(LoginRequest.class), anyString())).willReturn(result);
		given(authUserResponseMapper.toDisplayUserInfo(result.userInfo())).willReturn(displayUserInfo);

		mockMvc.perform(post("/api/mobile/auth/login")
						.header("Device-Id", "device-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"pw"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("로그인 성공"))
				.andExpect(jsonPath("$.user.id").value("user-1"))
				.andExpect(jsonPath("$.user.email").doesNotExist())
				.andExpect(jsonPath("$.user.avatarUrl")
						.value("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp"))
				.andExpect(jsonPath("$.tokens.accessToken").value("access-token"))
				.andExpect(jsonPath("$.tokens.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.tokens.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.tokens.accessTokenExpiresIn").value(1800L))
				.andExpect(jsonPath("$.tokens.refreshTokenExpiresIn").value(604800L));
	}

	@Test
	@DisplayName("POST /api/mobile/auth/login은 인증 실패 시 Problem Details를 반환한다")
	void loginReturnsProblemDetailWhenAuthenticationFails() throws Exception {
		given(loginUseCase.login(any(LoginRequest.class), anyString()))
				.willThrow(new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

		mockMvc.perform(post("/api/mobile/auth/login")
						.header("Device-Id", "device-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"wrong"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/security/invalid-credentials"))
				.andExpect(jsonPath("$.instance").value("/api/mobile/auth/login"));
	}

	@Test
	@DisplayName("POST /api/mobile/auth/reissue는 성공 시 tokens를 body로 반환한다")
	void reissueReturnsTokensBodyWhenSuccessful() throws Exception {
		ReissueResult result = new ReissueResult("new-access", "refresh-token", 1800L, 604800L);
		given(reissueTokenUseCase.reissueTokens("refresh-token")).willReturn(result);

		mockMvc.perform(post("/api/mobile/auth/reissue")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"refresh-token"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
				.andExpect(jsonPath("$.tokens.accessToken").value("new-access"))
				.andExpect(jsonPath("$.tokens.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.tokens.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.tokens.accessTokenExpiresIn").value(1800L))
				.andExpect(jsonPath("$.tokens.refreshTokenExpiresIn").value(604800L));
	}

	@Test
	@DisplayName("POST /api/mobile/auth/reissue는 실패 시 invalid-refresh-token Problem Details를 반환한다")
	void reissueReturnsProblemDetailWhenRefreshTokenIsInvalid() throws Exception {
		given(reissueTokenUseCase.reissueTokens("bad-token")).willReturn(null);

		mockMvc.perform(post("/api/mobile/auth/reissue")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"bad-token"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/security/invalid-refresh-token"))
				.andExpect(jsonPath("$.instance").value("/api/mobile/auth/reissue"));
	}

	@Test
	@DisplayName("POST /api/mobile/auth/logout은 성공 시 MessageResponse를 반환한다")
	void logoutReturnsMessageResponseWhenSuccessful() throws Exception {
		mockMvc.perform(post("/api/mobile/auth/logout")
						.header("Device-Id", "device-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"refresh-token"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("로그아웃 완료"));

		then(loginUseCase).should().logoutByRefreshToken("refresh-token", "device-1");
	}

	@Test
	@DisplayName("POST /api/mobile/auth/logout은 Device-Id가 없어도 성공을 반환한다")
	void logoutReturnsSuccessWhenDeviceIdIsMissing() throws Exception {
		mockMvc.perform(post("/api/mobile/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"refresh-token"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("로그아웃 완료"));

		then(loginUseCase).should().logoutByRefreshToken("refresh-token", null);
	}

	@Test
	@DisplayName("POST /api/mobile/auth/logout은 refresh token이 비어 있어도 성공을 반환한다")
	void logoutReturnsSuccessWhenRefreshTokenIsBlank() throws Exception {
		mockMvc.perform(post("/api/mobile/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":""}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("로그아웃 완료"));
	}
}
