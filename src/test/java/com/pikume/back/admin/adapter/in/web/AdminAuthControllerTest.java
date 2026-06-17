package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.in.web.dto.request.AdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.VerifyAdminOtpRequest;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
import com.pikume.back.admin.application.service.AdminAuthStep;
import com.pikume.back.admin.application.service.AdminLoginChallengeResult;
import com.pikume.back.admin.application.service.AdminTokenIssueResult;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.config.AdminUserDetails;
import com.pikume.back.security.jwt.AdminAuthConstants;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.jwt.SecurityTokenType;
import com.pikume.back.user.auth.constants.AuthConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthController")
class AdminAuthControllerTest {

	@Mock
	private AdminAuthUseCase adminAuthUseCase;
	@Mock
	private JwtProvider jwtProvider;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		AdminAuthController controller = new AdminAuthController(adminAuthUseCase, jwtProvider);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new AdminExceptionHandler(new ProblemDetailFactory()))
				.setCustomArgumentResolvers(new AdminPrincipalResolver(new AdminUserDetails(
						"admin-1",
						AdminRole.SUPER_ADMIN.name(),
						"session-1")))
				.build();
	}

	@Test
	@DisplayName("POST /api/admin/auth/login은 OTP challenge token을 반환한다")
	void loginReturnsOtpChallengeToken() throws Exception {
		given(adminAuthUseCase.login("ops-june", "AdminPass1!"))
				.willReturn(new AdminLoginChallengeResult(
						"otp-challenge-token",
						300L,
						AdminAuthStep.VERIFY_OTP.name(),
						"ops-june",
						"운영자1",
						"operator@pikume.com",
						AdminRole.OPERATOR));

		mockMvc.perform(post("/api/admin/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AdminLoginRequest(
								"ops-june",
								"AdminPass1!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.otpChallengeToken").value("otp-challenge-token"))
				.andExpect(jsonPath("$.nextStep").value(AdminAuthStep.VERIFY_OTP.name()));
	}

	@Test
	@DisplayName("POST /api/admin/auth/otp/verify는 Access Token 본문과 Refresh Token 쿠키를 반환한다")
	void verifyOtpReturnsAccessTokenAndRefreshTokenCookie() throws Exception {
		given(jwtProvider.validateToken("otp-challenge-token")).willReturn(true);
		given(jwtProvider.getTokenType("otp-challenge-token")).willReturn(SecurityTokenType.ADMIN_OTP_CHALLENGE);
		given(jwtProvider.getUserIdFromToken("otp-challenge-token")).willReturn("admin-1");
		given(adminAuthUseCase.verifyOtp("admin-1", "123456")).willReturn(tokenResult());

		mockMvc.perform(post("/api/admin/auth/otp/verify")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + "otp-challenge-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new VerifyAdminOtpRequest("123456"))))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + "access-token"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						org.hamcrest.Matchers.containsString(AdminAuthConstants.REFRESH_TOKEN_COOKIE_NAME + "=refresh-token")))
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").doesNotExist())
				.andExpect(jsonPath("$.admin.loginId").value("ops-june"));
	}

	@Test
	@DisplayName("POST /api/admin/auth/otp/verify는 OTP challenge token이 없으면 Problem Details를 반환한다")
	void verifyOtpReturnsProblemDetailsWhenChallengeTokenMissing() throws Exception {
		mockMvc.perform(post("/api/admin/auth/otp/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new VerifyAdminOtpRequest("123456"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/admin/otp-challenge-token-invalid"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/admin/auth/otp/verify"));
	}

	@Test
	@DisplayName("POST /api/admin/auth/logout은 현재 관리자 세션을 폐기한다")
	void logoutRevokesCurrentAdminSession() throws Exception {
		mockMvc.perform(post("/api/admin/auth/logout")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						org.hamcrest.Matchers.containsString(AdminAuthConstants.REFRESH_TOKEN_COOKIE_NAME + "=;")));

		then(adminAuthUseCase).should().logout("admin-1", "session-1");
	}

	private AdminTokenIssueResult tokenResult() {
		return new AdminTokenIssueResult(
				"access-token",
				"refresh-token",
				600L,
				1800L,
				"session-1",
				"ops-june",
				"운영자1",
				"operator@pikume.com",
				AdminRole.OPERATOR);
	}

	private record AdminPrincipalResolver(AdminUserDetails adminUserDetails) implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(AdminUserDetails.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return adminUserDetails;
		}
	}
}
