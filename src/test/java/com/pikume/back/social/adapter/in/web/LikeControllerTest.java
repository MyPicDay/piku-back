package com.pikume.back.social.adapter.in.web;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.social.adapter.in.web.problem.SocialProblemType;
import com.pikume.back.social.application.port.in.LikeUseCase;
import com.pikume.back.social.domain.like.exception.DuplicateLikeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeController")
class LikeControllerTest {

	@InjectMocks
	private LikeController likeController;

	@Mock
	private LikeUseCase likeUseCase;

	private MockMvc mockMvc;
	private CustomUserDetails userDetails;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		userDetails = new CustomUserDetails("user-1", "user");
		mockMvc = MockMvcBuilders.standaloneSetup(likeController)
				.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(userDetails))
				.setControllerAdvice(
						new GlobalExceptionHandler(java.util.Optional.empty(), problemDetailFactory),
						new SocialExceptionHandler(problemDetailFactory))
				.build();
	}

	@Test
	@DisplayName("POST /api/likes/diary/{diaryId}는 중복 좋아요 충돌 시 409를 반환한다")
	void addLikeReturnsConflictWhenDuplicateLikeExceptionOccurs() throws Exception {
		given(likeUseCase.addLike(eq("user-1"), anyLong()))
				.willThrow(new DuplicateLikeException("좋아요 중복 저장이 감지되었습니다.", null));

		mockMvc.perform(post("/api/likes/diary/1")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(SocialProblemType.DUPLICATE_LIKE.type().toString()))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("좋아요 중복 저장이 감지되었습니다."));
	}

	/*
	@Test
	@DisplayName("GET /api/likes/diary/{diaryId}/count는 비공개 일기 접근 시 404를 반환한다")
	void getLikeCountReturnsNotFoundWhenDiaryIsHidden() throws Exception {
		given(likeUseCase.getLikeCount("user-1", 1L))
				.willThrow(new LikeException(LikeErrorCode.DIARY_NOT_FOUND));

		mockMvc.perform(get("/api/likes/diary/1/count")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value(LikeErrorCode.DIARY_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("GET /api/likes/diary/{diaryId}는 application result를 web response로 변환한다")
	void getLikeStatusMapsApplicationResult() throws Exception {
		given(likeUseCase.getLikeStatus("user-1", 1L))
				.willReturn(new LikeResult(1L, 3L, true));

		mockMvc.perform(get("/api/likes/diary/1")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diaryId").value(1L))
				.andExpect(jsonPath("$.likeCount").value(3L))
				.andExpect(jsonPath("$.liked").value(true));
	}
	*/

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
