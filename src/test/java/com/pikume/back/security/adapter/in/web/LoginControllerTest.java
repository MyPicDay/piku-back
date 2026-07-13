package com.pikume.back.security.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.ProblemDetailFallbackExceptionResolver;
import com.pikume.back.global.util.CookieUtils;
import com.pikume.back.user.auth.application.dto.LoginCommand;
import com.pikume.back.user.auth.application.dto.LoginResult;
import com.pikume.back.user.auth.application.dto.ReissueSessionResult;
import com.pikume.back.user.auth.application.exception.InvalidCredentialsException;
import com.pikume.back.user.auth.application.port.in.LoginUseCase;
import com.pikume.back.user.auth.application.port.in.LogoutUseCase;
import com.pikume.back.user.auth.application.port.in.ReissueSessionUseCase;
import com.pikume.back.security.adapter.in.web.dto.request.LoginRequest;
import com.pikume.back.security.adapter.in.web.dto.response.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
	private ReissueSessionUseCase reissueSessionUseCase;

	@Mock
	private LogoutUseCase logoutUseCase;

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
				reissueSessionUseCase,
				logoutUseCase,
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
		given(loginUseCase.login(any(LoginCommand.class)))
				.willThrow(new InvalidCredentialsException());

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
		given(loginUseCase.login(any(LoginCommand.class)))
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
				"access-token", "refresh-token",
				new LoginResult.UserInfo(
						"user-1",
						"pikume",
						"public/characters/fixed/base_image_1.webp"));
		UserInfo displayUserInfo = new UserInfo(
				"user-1",
				"pikume",
				"https://assets.example.com/piku/public/characters/fixed/base_image_1.webp");
		given(loginUseCase.login(any(LoginCommand.class))).willReturn(loginResult);
		given(authUserResponseMapper.toDisplayUserInfo(loginResult.userInfo())).willReturn(displayUserInfo);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(header().string("Authorization", "Bearer access-token"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("rn=refresh-token")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=604800")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
				.andExpect(jsonPath("$.message").value("로그인 성공"))
				.andExpect(jsonPath("$.user.id").value("user-1"))
				.andExpect(jsonPath("$.user.email").doesNotExist())
				.andExpect(jsonPath("$.user.avatarUrl")
						.value("https://assets.example.com/piku/public/characters/fixed/base_image_1.webp"));
	}

	@Test
	@DisplayName("POST /api/auth/reissue는 실패 시 Problem Details를 반환한다")
	void reissueReturnsProblemDetailWhenRefreshTokenIsInvalid() throws Exception {
		given(cookieUtils.getCookieValue(any(), anyString())).willReturn("invalid-refresh-token");
		given(reissueSessionUseCase.reissueSession("invalid-refresh-token")).willReturn(null);

		mockMvc.perform(post("/api/auth/reissue"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().exists("Set-Cookie"))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/security/invalid-refresh-token"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/auth/reissue"));

		then(reissueSessionUseCase).should().reissueSession("invalid-refresh-token");
	}

	@Test
	@DisplayName("POST /api/auth/reissue는 성공 시 MessageResponse를 반환한다")
	void reissueReturnsMessageResponseWhenSuccessful() throws Exception {
		given(cookieUtils.getCookieValue(any(), anyString())).willReturn("valid-refresh-token");
		given(reissueSessionUseCase.reissueSession("valid-refresh-token"))
				.willReturn(new ReissueSessionResult("new-access-token", "valid-refresh-token", 1800L, 604800L));

		mockMvc.perform(post("/api/auth/reissue"))
				.andExpect(status().isOk())
				.andExpect(header().string("Authorization", "Bearer new-access-token"))
				.andExpect(jsonPath("$.message").value("토큰 재발급 성공"));

		then(reissueSessionUseCase).should().reissueSession("valid-refresh-token");
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
	void logoutReturnsMessageResponseWhenSuccessful() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthWebConstants.DEVICE_ID_HEADER, "ios");

		ResponseEntity<?> response = loginController.logout(
				new CustomUserDetails("user1", "pikume"),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().containsKey("Set-Cookie")).isTrue();
		assertThat(response.getBody()).isEqualTo(new MessageResponse("로그아웃 완료"));
		then(logoutUseCase).should().logout("user1", "ios");
	}
}
