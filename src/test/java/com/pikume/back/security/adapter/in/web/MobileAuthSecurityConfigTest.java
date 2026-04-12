package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.PikuBackApplication;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PikuBackApplication.class)
@AutoConfigureMockMvc
@DisplayName("MobileAuth SecurityConfig")
class MobileAuthSecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LoginUseCase loginUseCase;

	@MockBean
	private ReissueTokenUseCase reissueTokenUseCase;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("POST /api/mobile/auth/login은 인증 없이도 접근 가능하다")
	void mobileLoginIsPermittedWithoutAuthentication() throws Exception {
		LoginResult result = new LoginResult(
				new TokenDto("access-token", "refresh-token"),
				new UserInfo("user-1", "user@example.com", "pikume", "/avatar.png"));
		given(loginUseCase.login(any(), anyString())).willReturn(result);

		mockMvc.perform(post("/api/mobile/auth/login")
						.header("Device-Id", "device-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new com.pikume.back.security.dto.request.LoginRequest("user@example.com", "pw"))))
				.andExpect(status().isOk());
	}
}
