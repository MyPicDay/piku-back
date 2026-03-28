package com.pikume.back.security.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.global.util.CookieUtils;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;
import com.pikume.back.security.dto.request.LoginRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(loginController).build();
	}

	@Test
	@DisplayName("POST /api/auth/login은 로그인 실패 시 공용 에러 DTO를 반환한다")
	void loginReturnsErrorResponseWhenAuthenticationFails() throws Exception {
		LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
		given(loginUseCase.login(any(LoginRequest.class), nullable(String.class)))
				.willThrow(new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다."));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("로그인 실패: 이메일 또는 비밀번호가 올바르지 않습니다."));
	}
}
