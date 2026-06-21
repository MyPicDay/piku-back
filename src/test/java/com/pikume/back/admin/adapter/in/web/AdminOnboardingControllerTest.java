package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.in.web.dto.request.SetAdminCredentialsRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.TemporaryAdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.service.AdminOnboardingStep;
import com.pikume.back.admin.application.service.AdminAuthenticationResult;
import com.pikume.back.admin.application.service.AdminSessionCredentials;
import com.pikume.back.admin.application.service.AdminTemporaryLoginResult;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.config.AdminSecurityProperties;
import com.pikume.back.security.config.AdminSessionCookieManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOnboardingController")
class AdminOnboardingControllerTest {

	@Mock AdminOnboardingUseCase adminOnboardingUseCase;
	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		AdminSessionCookieManager manager = new AdminSessionCookieManager(new AdminSecurityProperties(
				List.of("http://localhost:3000"), "pk-a91f", "pk-b74d", "X-PK-C83F", false, ""));
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminOnboardingController(adminOnboardingUseCase, manager))
				.setControllerAdvice(new AdminExceptionHandler(
						new ProblemDetailFactory(), mock(AdminSessionTelemetryPort.class))).build();
	}

	@Test
	@DisplayName("임시 로그인은 사전 세션 쿠키를 사용하고 온보딩 토큰을 응답하지 않는다")
	void temporaryLoginUsesPreAuthenticationCookie() throws Exception {
		given(adminOnboardingUseCase.temporaryLogin("raw-session", "operator@pikume.com", "TempPass1!"))
				.willReturn(new AdminTemporaryLoginResult(AdminOnboardingStep.SET_CREDENTIALS.name(),
						"운영자1", AdminRole.OPERATOR));

		mockMvc.perform(post("/api/admin/auth/temporary-login")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new TemporaryAdminLoginRequest(
								"operator@pikume.com", "TempPass1!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingToken").doesNotExist())
				.andExpect(jsonPath("$.nextStep").value(AdminOnboardingStep.SET_CREDENTIALS.name()))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.loginId").doesNotExist());
	}

	@Test
	@DisplayName("온보딩 OTP 성공 응답은 이메일과 로그인 아이디를 노출하지 않는다")
	void verifyOtpDoesNotExposeIdentifiers() throws Exception {
		given(adminOnboardingUseCase.verifyOtp("raw-session", "123456"))
				.willReturn(new AdminAuthenticationResult(
						new AdminSessionCredentials("new-session", "new-csrf"),
						"운영자1",
						AdminRole.OPERATOR));

		mockMvc.perform(post("/api/admin/auth/onboarding/otp/verify")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"otpCode\":\"123456\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true))
				.andExpect(jsonPath("$.admin.nickname").value("운영자1"))
				.andExpect(jsonPath("$.admin.role").value(AdminRole.OPERATOR.name()))
				.andExpect(jsonPath("$.admin.email").doesNotExist())
				.andExpect(jsonPath("$.admin.loginId").doesNotExist());
	}

	@Test
	@DisplayName("로그인 아이디와 패스워드 설정은 하나의 요청과 사전 세션 쿠키를 사용한다")
	void setCredentialsUsesPreAuthenticationCookie() throws Exception {
		mockMvc.perform(patch("/api/admin/auth/onboarding/credentials")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new SetAdminCredentialsRequest("ops-june", "Password1!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nextStep").value(AdminOnboardingStep.REGISTER_OTP.name()));

		then(adminOnboardingUseCase).should().setCredentials("raw-session", "ops-june", "Password1!");
	}

	@Test
	@DisplayName("기존 로그인 아이디와 패스워드 개별 설정 경로는 제거한다")
	void removesSeparateCredentialEndpoints() throws Exception {
		mockMvc.perform(patch("/api/admin/auth/onboarding/login-id")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/admin/auth/onboarding/password")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());
	}
}
