package com.pikume.back.social.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
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
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.social.adapter.in.web.problem.SocialProblemType;
import com.pikume.back.social.application.port.in.CommentUseCase;
import com.pikume.back.social.domain.comment.exception.CommentErrorCode;
import com.pikume.back.social.domain.comment.exception.CommentException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController")
class CommentControllerTest {

	@InjectMocks
	private CommentController commentController;

	@Mock
	private CommentUseCase commentUseCase;

	@Mock
	private RequestMetaMapper requestMetaMapper;

	private MockMvc mockMvc;
	private RequestMetaInfo requestMetaInfo;
	private CustomUserDetails userDetails;

	@BeforeEach
	void setUp() {
		userDetails = new CustomUserDetails("viewer-id", "viewer@example.com", "viewer");
		mockMvc = MockMvcBuilders.standaloneSetup(commentController)
				.setCustomArgumentResolvers(
						new AuthenticationPrincipalResolver(userDetails),
						new PageableHandlerMethodArgumentResolver())
				.setControllerAdvice(new SocialExceptionHandler(new ProblemDetailFactory()))
				.build();
		requestMetaInfo = new RequestMetaInfo(
				"https",
				"localhost",
				8080,
				"localhost:8080",
				"https://localhost:8080/api/comments",
				"JUnit",
				"127.0.0.1");
	}

	@Test
	@DisplayName("GET /api/comments는 비공개 일기 접근 시 404를 반환한다")
	void getRootCommentsReturnsNotFoundWhenDiaryIsHidden() throws Exception {
		given(requestMetaMapper.extractMetaInfo(any(HttpServletRequest.class))).willReturn(requestMetaInfo);
		given(commentUseCase.getRootCommentsByDiaryId(eq(1L), any(), eq(requestMetaInfo), eq("viewer-id")))
				.willThrow(new CommentException(CommentErrorCode.DIARY_NOT_FOUND));

		mockMvc.perform(get("/api/comments")
						.param("diaryId", "1")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value(SocialProblemType.DIARY_NOT_FOUND.type().toString()))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value(CommentErrorCode.DIARY_NOT_FOUND.getMessage()));
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
