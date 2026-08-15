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
import com.pikume.back.user.application.exception.UserAvatarReferenceIntegrityException;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;

import java.util.List;

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

	@Test
	@DisplayName("NicknameAlreadyExistsException은 nickname-conflict Problem Details로 변환된다")
	void nicknameConflict() throws Exception {
		mockMvc.perform(get("/test/nickname-conflict"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value(UserProblemType.NICKNAME_CONFLICT.type().toString()))
				.andExpect(jsonPath("$.title").value(UserProblemType.NICKNAME_CONFLICT.title()))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("이미 사용 중인 닉네임입니다."))
				.andExpect(jsonPath("$.instance").value("/test/nickname-conflict"));
	}

	@Test
	@DisplayName("아바타 캐릭터 정합성 오류는 내부 식별자를 숨긴 Problem Details로 변환된다")
	void avatarReferenceIntegrityFailure() throws Exception {
		mockMvc.perform(get("/test/avatar-reference-integrity"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.type").value(
						UserProblemType.AVATAR_REFERENCE_INTEGRITY.type().toString()))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value(
						UserErrorCode.AVATAR_CHARACTER_REFERENCE_INTEGRITY_VIOLATION.getMessage()))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("42"))));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/user-not-found")
		void userNotFound() {
			throw new UserNotFoundException();
		}

		@GetMapping("/test/nickname-conflict")
		void nicknameConflict() {
			throw new NicknameAlreadyExistsException("duplicate-nickname");
		}

		@GetMapping("/test/avatar-reference-integrity")
		void avatarReferenceIntegrityFailure() {
			throw new UserAvatarReferenceIntegrityException(List.of(42L));
		}
	}
}
