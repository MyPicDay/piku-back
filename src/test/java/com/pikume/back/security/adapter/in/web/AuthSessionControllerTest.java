package com.pikume.back.security.adapter.in.web;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.security.adapter.in.web.problem.SecurityProblemType;
import com.pikume.back.security.dto.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthSessionController")
class AuthSessionControllerTest {

	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
	private final AuthUserResponseMapper authUserResponseMapper = org.mockito.Mockito.mock(AuthUserResponseMapper.class);

	@Test
	@DisplayName("GET /api/auth/me는 인증 사용자를 LoginResponse 형태로 반환한다")
	void getCurrentUserReturnsLoginResponseShape() throws Exception {
		CustomUserDetails userDetails = CustomUserDetails.withAvatarPath(
				"user-1",
				"user@example.com",
				"pikume",
				"public/characters/fixed/base_image_1.png");
		UserInfo displayUserInfo = new UserInfo(
				"user-1",
				"user@example.com",
				"pikume",
				"https://assets.example.com/piku/public/characters/fixed/base_image_1.png");
		org.mockito.BDDMockito.given(authUserResponseMapper.toDisplayUserInfo(userDetails)).willReturn(displayUserInfo);
		MockMvc mockMvc = mockMvcWith(userDetails);

		mockMvc.perform(get("/api/auth/me")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("토큰 검증 성공"))
				.andExpect(jsonPath("$.user.id").value("user-1"))
				.andExpect(jsonPath("$.user.email").value("user@example.com"))
				.andExpect(jsonPath("$.user.nickname").value("pikume"))
				.andExpect(jsonPath("$.user.avatarUrl")
						.value("https://assets.example.com/piku/public/characters/fixed/base_image_1.png"));
	}

	@Test
	@DisplayName("GET /api/auth/me는 인증 사용자가 없으면 Problem Details 401을 반환한다")
	void getCurrentUserReturnsProblemDetailWhenPrincipalMissing() throws Exception {
		MockMvc mockMvc = mockMvcWith(null);

		mockMvc.perform(get("/api/auth/me")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.type").value(SecurityProblemType.UNAUTHENTICATED.type().toString()))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
				.andExpect(jsonPath("$.instance").value("/api/auth/me"));
	}

	private MockMvc mockMvcWith(CustomUserDetails userDetails) {
		AuthSessionController authSessionController = new AuthSessionController(problemDetailFactory, authUserResponseMapper);
		return MockMvcBuilders.standaloneSetup(authSessionController)
				.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(userDetails))
				.build();
	}

	private record AuthenticationPrincipalResolver(CustomUserDetails userDetails) implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(CustomUserDetails.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return userDetails;
		}
	}
}
