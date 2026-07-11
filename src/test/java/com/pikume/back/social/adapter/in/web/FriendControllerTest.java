package com.pikume.back.social.adapter.in.web;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.social.adapter.in.web.problem.SocialProblemType;
import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.social.domain.friend.exception.AlreadyFriendsException;
import com.pikume.back.social.domain.friend.exception.FriendException;
import com.pikume.back.social.domain.friend.exception.FriendNotFoundException;
import com.pikume.back.social.domain.friend.exception.FriendRequestNotFoundException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendController")
class FriendControllerTest {

	@InjectMocks
	private FriendController friendController;

	@Mock
	private FriendUseCase friendUseCase;

	private MockMvc mockMvc;
	private CustomUserDetails userDetails;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		userDetails = new CustomUserDetails("user-1", "user");
		mockMvc = MockMvcBuilders.standaloneSetup(friendController)
				.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(userDetails))
				.setControllerAdvice(
						new GlobalExceptionHandler(java.util.Optional.empty(), problemDetailFactory),
						new SocialExceptionHandler(problemDetailFactory))
				.build();
	}

	@Test
	@DisplayName("POST /api/relation은 잘못된 친구 요청 시 400 Problem Details를 반환한다")
	void sendFriendRequestReturnsBadRequestProblemDetail() throws Exception {
		given(friendUseCase.sendFriendRequest(eq("user-1"), eq("user-2")))
				.willThrow(new FriendException("자신에게 요청 할 수 없습니다."));

		mockMvc.perform(post("/api/relation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"toUserId\":\"user-2\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(SocialProblemType.INVALID_FRIEND_REQUEST.type().toString()))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("자신에게 요청 할 수 없습니다."));
	}

	@Test
	@DisplayName("POST /api/relation은 이미 친구인 경우 409 Problem Details를 반환한다")
	void sendFriendRequestReturnsConflictProblemDetailWhenAlreadyFriends() throws Exception {
		given(friendUseCase.sendFriendRequest(eq("user-1"), eq("user-2")))
				.willThrow(new AlreadyFriendsException("이미 친구입니다."));

		mockMvc.perform(post("/api/relation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"toUserId\":\"user-2\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(SocialProblemType.ALREADY_FRIENDS.type().toString()))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("이미 친구입니다."));
	}

	@Test
	@DisplayName("DELETE /api/relation/requests/{fromUserId}는 친구 요청이 없으면 404 Problem Details를 반환한다")
	void rejectFriendRequestReturnsNotFoundProblemDetail() throws Exception {
		given(friendUseCase.rejectFriendRequest("user-1", "user-2"))
				.willThrow(new FriendRequestNotFoundException("해당 친구 요청 기록을 찾을 수 없습니다."));

		mockMvc.perform(delete("/api/relation/requests/user-2"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value(SocialProblemType.FRIEND_REQUEST_NOT_FOUND.type().toString()))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("해당 친구 요청 기록을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("DELETE /api/relation/cancel/{toUserId}는 보낸 친구 요청이 없으면 404 Problem Details를 반환한다")
	void cancelFriendRequestReturnsNotFoundProblemDetail() throws Exception {
		given(friendUseCase.cancelFriendRequest("user-1", "user-2"))
				.willThrow(new FriendRequestNotFoundException("요청 보낸 기록이 없습니다."));

		mockMvc.perform(delete("/api/relation/cancel/user-2"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value(SocialProblemType.FRIEND_REQUEST_NOT_FOUND.type().toString()))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("요청 보낸 기록이 없습니다."));
	}

	@Test
	@DisplayName("DELETE /api/relation/{toUserId}는 친구 관계가 없으면 404 Problem Details를 반환한다")
	void removeFriendReturnsNotFoundProblemDetail() throws Exception {
		given(friendUseCase.removeFriend("user-1", "user-2"))
				.willThrow(new FriendNotFoundException("친구 관계가 존재하지 않습니다."));

		mockMvc.perform(delete("/api/relation/user-2"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value(SocialProblemType.FRIEND_NOT_FOUND.type().toString()))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("친구 관계가 존재하지 않습니다."));
	}

	private record AuthenticationPrincipalResolver(CustomUserDetails userDetails)
			implements HandlerMethodArgumentResolver {
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
