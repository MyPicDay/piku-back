package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.in.web.dto.request.SetAdminLoginIdRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.TemporaryAdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminOnboardingUseCase;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.application.service.AdminOnboardingStep;
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
				.willReturn(new AdminTemporaryLoginResult(AdminOnboardingStep.SET_LOGIN_ID.name(),
						"operator@pikume.com", "운영자1", AdminRole.OPERATOR));

		mockMvc.perform(post("/api/admin/auth/temporary-login")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new TemporaryAdminLoginRequest(
								"operator@pikume.com", "TempPass1!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingToken").doesNotExist())
				.andExpect(jsonPath("$.nextStep").value(AdminOnboardingStep.SET_LOGIN_ID.name()));
	}

	@Test
	@DisplayName("로그인 아이디 설정은 JWT 없이 사전 세션 쿠키를 사용한다")
	void setLoginIdUsesPreAuthenticationCookie() throws Exception {
		mockMvc.perform(patch("/api/admin/auth/onboarding/login-id")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SetAdminLoginIdRequest("ops-june"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nextStep").value(AdminOnboardingStep.SET_PASSWORD.name()));

		then(adminOnboardingUseCase).should().setLoginId("raw-session", "ops-june");
	}
}
