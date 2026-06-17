package com.pikume.back.security.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.admin.adapter.out.persistence.AdminSessionJpaRepository;
import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminRole;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
	private AdminSessionJpaRepository adminSessionJpaRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CorsConfigurationSource corsConfigurationSource;

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

	@Test
	@DisplayName("사용자 토큰으로 관리자 API에 접근할 수 없다")
	void userTokenCannotAccessAdminApi() throws Exception {
		User user = userJpaRepository.saveAndFlush(new User(
				"admin-boundary-user@example.com",
				"encoded-password",
				"boundary-user",
				"public/characters/fixed/base_image_1.webp"));
		String accessToken = jwtProvider.generateAccessToken(user.getId());

		mockMvc.perform(get("/api/admin/statistics/dashboard")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + accessToken)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value(SecurityProblemType.UNAUTHENTICATED.type().toString()))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/admin/statistics/dashboard"));
	}

	@Test
	@DisplayName("관리자 토큰으로 일반 사용자 API에 접근할 수 없다")
	void adminTokenCannotAccessUserApi() throws Exception {
		String accessToken = jwtProvider.generateAdminAccessToken(
				"018f6f6a-5d8c-7c4f-9d4f-7b7db85b26b1",
				AdminRole.SUPER_ADMIN.name(),
				"session-1");

		mockMvc.perform(get("/api/auth/me")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + accessToken)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value(SecurityProblemType.UNAUTHENTICATED.type().toString()))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/auth/me"));
	}

	@Test
	@DisplayName("관리자 토큰은 관리자 보호 경로의 보안 검사를 통과한다")
	void adminTokenPassesAdminSecurityBoundary() throws Exception {
		String adminId = "018f6f6a-5d8c-7c4f-9d4f-7b7db85b26b1";
		String sessionId = "018f6f6a-5d8c-7c4f-9d4f-7b7db85b26b2";
		LocalDateTime now = LocalDateTime.now();
		adminSessionJpaRepository.saveAndFlush(AdminSession.start(
				sessionId,
				adminId,
				"refresh-token-hash",
				now.plusHours(1),
				now.plusMinutes(30),
				now));
		String accessToken = jwtProvider.generateAdminAccessToken(
				adminId,
				AdminRole.SUPER_ADMIN.name(),
				sessionId);

		mockMvc.perform(get("/api/admin/statistics/dashboard")
						.header(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + accessToken)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("관리자 API CORS는 관리자 Origin만 허용한다")
	void adminCorsAllowsOnlyAdminOrigin() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/statistics/dashboard");

		CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

		assertThat(configuration).isNotNull();
		assertThat(configuration.getAllowedOrigins()).containsExactly("https://pikume-ops.pikume.com");
		assertThat(configuration.getAllowedOrigins()).doesNotContain("https://www.pikume.com");
	}

	@Test
	@DisplayName("상태를 변경하는 관리자 API 요청은 관리자 Origin만 허용한다")
	void adminStateChangingRequestsRequireAllowedOrigin() throws Exception {
		mockMvc.perform(post("/api/admin/auth/temporary-login")
						.header(HttpHeaders.ORIGIN, "https://www.pikume.com")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/admin/origin-forbidden"))
				.andExpect(jsonPath("$.status").value(403));
	}
}
