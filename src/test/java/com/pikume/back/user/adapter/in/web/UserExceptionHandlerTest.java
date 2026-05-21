package com.pikume.back.user.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.user.adapter.in.web.problem.UserProblemType;
import com.pikume.back.user.application.exception.UserErrorCode;
import com.pikume.back.user.application.exception.UserNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("UserExceptionHandler")
class UserExceptionHandlerTest {

	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new UserExceptionHandler(new ProblemDetailFactory()))
			.build();

	@Test
	@DisplayName("UserNotFoundException은 user not-found Problem Details로 변환된다")
	void userNotFound() throws Exception {
		mockMvc.perform(get("/test/user-not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value(UserProblemType.NOT_FOUND.type().toString()))
				.andExpect(jsonPath("$.title").value(UserProblemType.NOT_FOUND.title()))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value(UserErrorCode.USER_NOT_FOUND.getMessage()))
				.andExpect(jsonPath("$.instance").value("/test/user-not-found"));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/user-not-found")
		void userNotFound() {
			throw new UserNotFoundException();
		}
	}
}
