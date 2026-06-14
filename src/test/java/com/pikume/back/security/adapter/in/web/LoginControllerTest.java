package com.pikume.back.security.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.CookieSpec;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.ProblemDetailFallbackExceptionResolver;
import com.pikume.back.global.util.CookieUtils;
import com.pikume.back.security.application.dto.AuthenticatedUserInfo;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.dto.ReissueResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.request.LoginRequest;
import com.pikume.back.security.dto.UserInfo;
import com.pikume.back.user.auth.constants.AuthConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginController")
class LoginControllerTest {

	@InjectMocks
	private LoginController loginController;

	@Mock
	private LoginUseCase loginUseCase;

	@Mock
	private ReissueTokenUseCase reissueTokenUseCase;

	@Mock
	private CookieUtils cookieUtils;

	@Mock
	private AuthUserResponseMapper authUserResponseMapper;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		loginController = new LoginController(
				loginUseCase,
				reissueTokenUseCase,
				cookieUtils,
				problemDetailFactory,
				authUserResponseMapper);
		mockMvc = MockMvcBuilders.standaloneSetup(loginController)
				.setHandlerExceptionResolvers(new ProblemDetailFallbackExceptionResolver(
						objectMapper,
						problemDetailFactory,
						java.util.Optional.empty()))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@Test
	@DisplayName("POST /api/auth/login은 로그인 실패 시 Problem Details를 반환한다")
	void loginReturnsProblemDetailWhenAuthenticationFails() throws Exception {
		LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
		given(loginUseCase.login(any(LoginRequest.class), nullable(String.class)))
				.willThrow(new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/security/invalid-credentials"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다."))
				.andExpect(jsonPath("$.instance").value("/api/auth/login"));
	}

	@Test
	@DisplayName("POST /api/auth/login은 예상치 못한 런타임 예외를 500 Problem Details로 반환한다")
	void loginReturnsInternalServerErrorWhenUnexpectedRuntimeOccurs() throws Exception {
		LoginRequest request = new LoginRequest("user@example.com", "password");
		given(loginUseCase.login(any(LoginRequest.class), nullable(String.class)))
				.willThrow(new IllegalStateException("리프레시 토큰 저장 실패"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.type").value(CommonProblemType.INTERNAL_SERVER_ERROR.type().toString()))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("서버에 오류가 발생했습니다."))
				.andExpect(jsonPath("$.instance").value("/api/auth/login"));
	}

	@Test
	@DisplayName("POST /api/auth/login은 성공 시 login result의 사용자 정보를 사용한다")
	void loginReturnsSuccessWithoutAdditionalLookup() throws Exception {
		LoginRequest request = new LoginRequest("user@example.com", "password");
		LoginResult loginResult = new LoginResult(
				new TokenDto("access-token", "refresh-token"),
				new AuthenticatedUserInfo(
						"user-1",
						"user@example.com",
						"pikume",
						"public/characters/fixed/base_image_1.png"));
		UserInfo displayUserInfo = new UserInfo(
				"user-1",
				"user@example.com",
				"pikume",
				"https://assets.example.com/piku/public/characters/fixed/base_image_1.png");
		CookieSpec cookieSpec = new CookieSpec("refreshToken", "refresh-token", true, true, "/", 3600, "Lax");
		given(loginUseCase.login(any(LoginRequest.class), nullable(String.class))).willReturn(loginResult);
		given(loginUseCase.newCookieRefreshToken("refresh-token")).willReturn(cookieSpec);
		given(authUserResponseMapper.toDisplayUserInfo(loginResult.userInfo())).willReturn(displayUserInfo);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(header().string("Authorization", "Bearer access-token"))
				.andExpect(jsonPath("$.message").value("로그인 성공"))
				.andExpect(jsonPath("$.user.id").value("user-1"))
				.andExpect(jsonPath("$.user.email").value("user@example.com"))
				.andExpect(jsonPath("$.user.avatarUrl")
						.value("https://assets.example.com/piku/public/characters/fixed/base_image_1.png"));
	}

	@Test
	@DisplayName("POST /api/auth/reissue는 실패 시 Problem Details를 반환한다")
	void reissueReturnsProblemDetailWhenRefreshTokenIsInvalid() throws Exception {
		CookieSpec deleteCookie = new CookieSpec("refreshToken", "", true, true, "/", 0, "None");
		given(cookieUtils.getCookieValue(any(), anyString())).willReturn("invalid-refresh-token");
		given(reissueTokenUseCase.reissueTokens("invalid-refresh-token")).willReturn(null);
		given(loginUseCase.removeCookieRefreshToken()).willReturn(deleteCookie);

		mockMvc.perform(post("/api/auth/reissue"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().exists("Set-Cookie"))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/security/invalid-refresh-token"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/auth/reissue"));

		then(reissueTokenUseCase).should().reissueTokens("invalid-refresh-token");
	}

	@Test
	@DisplayName("POST /api/auth/reissue는 성공 시 MessageResponse를 반환한다")
	void reissueReturnsMessageResponseWhenSuccessful() throws Exception {
		given(cookieUtils.getCookieValue(any(), anyString())).willReturn("valid-refresh-token");
		given(reissueTokenUseCase.reissueTokens("valid-refresh-token"))
				.willReturn(new ReissueResult("new-access-token", "valid-refresh-token", 1800L, 604800L));

		mockMvc.perform(post("/api/auth/reissue"))
				.andExpect(status().isOk())
				.andExpect(header().string("Authorization", "Bearer new-access-token"))
				.andExpect(jsonPath("$.message").value("토큰 재발급 성공"));

		then(reissueTokenUseCase).should().reissueTokens("valid-refresh-token");
	}

	@Test
	@DisplayName("POST /api/auth/logout은 비로그인 상태면 Problem Details를 반환한다")
	void logoutReturnsProblemDetailWhenUnauthenticated() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/security/unauthenticated"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/auth/logout"));
	}

	@Test
	@DisplayName("POST /api/auth/logout은 성공 시 MessageResponse를 반환한다")
	void logoutReturnsMessageResponseWhenSuccessful() throws Exception {
		CookieSpec deleteCookie = new CookieSpec("refreshToken", "", true, true, "/", 0, "None");
		given(loginUseCase.removeCookieRefreshToken()).willReturn(deleteCookie);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthConstants.DEVICE_ID_HEADER, "ios");

		ResponseEntity<?> response = loginController.logout(
				new CustomUserDetails("user1", "user@example.com", "pikume"),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().containsKey("Set-Cookie")).isTrue();
		assertThat(response.getBody()).isEqualTo(new MessageResponse("로그아웃 완료"));
		then(loginUseCase).should().logout("user1", "ios");
	}
}
