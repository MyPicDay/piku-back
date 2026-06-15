package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.auth.constants.AuthConstants;
import com.pikume.back.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Auth session security")
class AuthSessionSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("GET /api/auth/me는 토큰이 없으면 Problem Details 401을 반환한다")
	void getCurrentUserRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/me")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value(SecurityProblemType.UNAUTHENTICATED.type().toString()))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/auth/me"));
	}

	@Test
	@DisplayName("GET /api/auth/me는 유효한 Bearer 토큰의 사용자 정보를 반환한다")
	void getCurrentUserReturnsUserForBearerTokenSubjectUserId() throws Exception {
		User user = userJpaRepository.saveAndFlush(new User(
				"session-user@example.com",
				"encoded-password",
				"session-user",
				"public/characters/fixed/base_image_1.webp"));
		String accessToken = jwtProvider.generateAccessToken(user.getId());

		mockMvc.perform(get("/api/auth/me")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + accessToken)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("토큰 검증 성공"))
				.andExpect(jsonPath("$.user.id").value(user.getId()))
				.andExpect(jsonPath("$.user.email").doesNotExist())
				.andExpect(jsonPath("$.user.nickname").value(user.getNickname()))
				.andExpect(jsonPath("$.user.avatarUrl")
						.value("http://localhost:9000/piku/public/characters/fixed/base_image_1.webp"));
	}

	@Test
	@DisplayName("POST /api/auth/login은 인증 없이도 로그인 컨트롤러까지 도달한다")
	void loginRemainsPublicAfterAuthMatcherTightening() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", "missing@example.com",
								"password", "wrong-password")))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value(SecurityProblemType.INVALID_CREDENTIALS.type().toString()))
				.andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다."));
	}
}
