package com.pikume.back.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.in.web.dto.request.AdminLoginRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.VerifyAdminOtpRequest;
import com.pikume.back.admin.adapter.in.web.problem.AdminExceptionHandler;
import com.pikume.back.admin.application.port.in.AdminAuthUseCase;
import com.pikume.back.admin.application.port.in.RecordAdminSecurityEventUseCase;
import com.pikume.back.admin.application.service.AdminAuthStep;
import com.pikume.back.admin.application.service.AdminAuthenticationResult;
import com.pikume.back.admin.application.service.AdminLoginChallengeResult;
import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthController")
class AdminAuthControllerTest {

	@Mock AdminAuthUseCase adminAuthUseCase;
	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		AdminSessionCookieManager manager = new AdminSessionCookieManager(properties());
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminAuthController(adminAuthUseCase, manager))
				.setControllerAdvice(new AdminExceptionHandler(
						new ProblemDetailFactory(), mock(RecordAdminSecurityEventUseCase.class))).build();
	}

	@Test
	@DisplayName("로그인은 사전 세션 쿠키를 사용하고 토큰을 응답하지 않는다")
	void loginUsesPreAuthenticationCookie() throws Exception {
		given(adminAuthUseCase.login("raw-session", "ops-june", "AdminPass1!"))
				.willReturn(new AdminLoginChallengeResult(AdminAuthStep.VERIFY_OTP.name()));

		mockMvc.perform(post("/api/admin/auth/login")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AdminLoginRequest("ops-june", "AdminPass1!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.otpChallengeToken").doesNotExist())
				.andExpect(jsonPath("$.nextStep").value(AdminAuthStep.VERIFY_OTP.name()))
				.andExpect(jsonPath("$.loginId").doesNotExist())
				.andExpect(jsonPath("$.nickname").doesNotExist())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.role").doesNotExist());
	}

	@Test
	@DisplayName("OTP 성공은 Access Token 없이 세션과 CSRF 쿠키를 교체한다")
	void verifyOtpRotatesCookies() throws Exception {
		given(adminAuthUseCase.verifyOtp("raw-session", "123456")).willReturn(authenticationResult());

		var response = mockMvc.perform(post("/api/admin/auth/otp/verify")
						.cookie(new Cookie("pk-a91f", "raw-session"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new VerifyAdminOtpRequest("123456"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("운영자1"))
				.andExpect(jsonPath("$.role").value(AdminRole.OPERATOR.name()))
				.andExpect(jsonPath("$.authenticated").doesNotExist())
				.andExpect(jsonPath("$.admin").doesNotExist())
				.andExpect(jsonPath("$.loginId").doesNotExist())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andReturn().getResponse();

		assertThat(response.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
		assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
				.anyMatch(value -> value.startsWith("pk-a91f=new-session"))
				.anyMatch(value -> value.startsWith("pk-b74d=new-csrf"));
	}

	@Test
	@DisplayName("사전 세션 쿠키가 없으면 Problem Details 401을 반환한다")
	void loginRequiresPreAuthenticationCookie() throws Exception {
		mockMvc.perform(post("/api/admin/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AdminLoginRequest("ops-june", "AdminPass1!"))))
				.andExpect(status().isUnauthorized())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(jsonPath("$.status").value(401));
	}

	private AdminAuthenticationResult authenticationResult() {
		return new AdminAuthenticationResult(new AdminSessionCredentialResult("new-session", "new-csrf"),
				"운영자1", AdminRole.OPERATOR);
	}

	private AdminSecurityProperties properties() {
		return new AdminSecurityProperties(List.of("http://localhost:3000"),
				"pk-a91f", "pk-b74d", "X-PK-C83F", false, "");
	}
}
