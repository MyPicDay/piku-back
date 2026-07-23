package com.pikume.back.social.adapter.in.web;

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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.social.adapter.in.web.problem.SocialProblemType;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.in.CreateCommentUseCase;
import com.pikume.back.social.application.port.in.DeleteCommentUseCase;
import com.pikume.back.social.application.port.in.QueryCommentPageUseCase;
import com.pikume.back.social.application.port.in.UpdateCommentUseCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController")
class CommentControllerTest {

	@InjectMocks
	private CommentController commentController;

	@Mock
	private CreateCommentUseCase createCommentUseCase;
	@Mock
	private UpdateCommentUseCase updateCommentUseCase;
	@Mock
	private DeleteCommentUseCase deleteCommentUseCase;
	@Mock
	private QueryCommentPageUseCase queryCommentPageUseCase;

	private MockMvc mockMvc;
	private CustomUserDetails userDetails;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		userDetails = new CustomUserDetails("viewer-id", "viewer");
		mockMvc = MockMvcBuilders.standaloneSetup(commentController)
				.setCustomArgumentResolvers(
						new AuthenticationPrincipalResolver(userDetails),
						new PageableHandlerMethodArgumentResolver())
				.setControllerAdvice(
						new GlobalExceptionHandler(java.util.Optional.empty(), problemDetailFactory),
						new SocialExceptionHandler(problemDetailFactory))
				.build();
	}

	@Test
	@DisplayName("GET /api/comments는 비공개 일기 접근 시 404를 반환한다")
	void getRootCommentsReturnsNotFoundWhenDiaryIsHidden() throws Exception {
		given(queryCommentPageUseCase.queryRootCommentPage(eq(1L), any(), eq("viewer-id")))
				.willThrow(new SocialException(SocialErrorCode.DIARY_NOT_FOUND));

		mockMvc.perform(get("/api/comments")
						.param("diaryId", "1")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value(SocialProblemType.DIARY_NOT_FOUND.type().toString()))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value(SocialErrorCode.DIARY_NOT_FOUND.message()));
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
