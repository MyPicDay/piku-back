package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.in.web.dto.request.SetAdminLoginIdRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.TemporaryAdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.service.AdminOnboardingStep;
import com.pikume.back.admin.application.service.AdminTemporaryLoginResult;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.jwt.SecurityTokenType;
import com.pikume.back.user.auth.constants.AuthConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOnboardingController")
class AdminOnboardingControllerTest {

	@Mock
	private AdminOnboardingUseCase adminOnboardingUseCase;
	@Mock
	private JwtProvider jwtProvider;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		AdminOnboardingController controller = new AdminOnboardingController(adminOnboardingUseCase, jwtProvider);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new AdminExceptionHandler(new ProblemDetailFactory()))
				.build();
	}

	@Test
	@DisplayName("POST /api/admin/auth/temporary-login은 온보딩 토큰과 다음 단계를 반환한다")
	void temporaryLoginReturnsOnboardingToken() throws Exception {
		given(adminOnboardingUseCase.temporaryLogin("operator@pikume.com", "TempPass1!"))
				.willReturn(new AdminTemporaryLoginResult(
						"onboarding-token",
						600L,
						AdminOnboardingStep.SET_LOGIN_ID.name(),
						"operator@pikume.com",
						"운영자1",
						AdminRole.OPERATOR));

		mockMvc.perform(post("/api/admin/auth/temporary-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new TemporaryAdminLoginRequest(
								"operator@pikume.com",
								"TempPass1!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingToken").value("onboarding-token"))
				.andExpect(jsonPath("$.nextStep").value(AdminOnboardingStep.SET_LOGIN_ID.name()));
	}

	@Test
	@DisplayName("PATCH /api/admin/auth/onboarding/login-id는 온보딩 토큰을 검증하고 다음 단계를 반환한다")
	void setLoginIdReturnsNextStep() throws Exception {
		given(jwtProvider.validateToken("onboarding-token")).willReturn(true);
		given(jwtProvider.getTokenType("onboarding-token")).willReturn(SecurityTokenType.ADMIN_ONBOARDING);
		given(jwtProvider.getUserIdFromToken("onboarding-token")).willReturn("admin-1");

		mockMvc.perform(patch("/api/admin/auth/onboarding/login-id")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + "onboarding-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SetAdminLoginIdRequest("ops-june"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nextStep").value(AdminOnboardingStep.SET_PASSWORD.name()));

		then(adminOnboardingUseCase).should().setLoginId("admin-1", "ops-june");
	}

	@Test
	@DisplayName("PATCH /api/admin/auth/onboarding/login-id는 잘못된 토큰이면 Problem Details를 반환한다")
	void setLoginIdReturnsProblemDetailsWhenTokenIsInvalid() throws Exception {
		given(jwtProvider.validateToken("bad-token")).willReturn(false);

		mockMvc.perform(patch("/api/admin/auth/onboarding/login-id")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + "bad-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SetAdminLoginIdRequest("ops-june"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/admin/onboarding-token-invalid"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/admin/auth/onboarding/login-id"));
	}

	@Test
	@DisplayName("PATCH /api/admin/auth/onboarding/login-id는 토큰이 없으면 Problem Details를 반환한다")
	void setLoginIdReturnsProblemDetailsWhenTokenIsMissing() throws Exception {
		mockMvc.perform(patch("/api/admin/auth/onboarding/login-id")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SetAdminLoginIdRequest("ops-june"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/admin/onboarding-token-invalid"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/admin/auth/onboarding/login-id"));
	}
}
