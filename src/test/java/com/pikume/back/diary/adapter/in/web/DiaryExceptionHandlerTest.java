package com.pikume.back.diary.adapter.in.web;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pikume.back.diary.domain.exception.DiaryNotFoundException;
import com.pikume.back.diary.domain.exception.DuplicateDiaryException;
import com.pikume.back.global.error.ProblemDetailFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DiaryExceptionHandler")
class DiaryExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(new DiaryExceptionHandler(new ProblemDetailFactory()))
				.build();
	}

	@Test
	@DisplayName("DiaryNotFoundException은 diary 전용 Problem Details를 반환한다")
	void handlesDiaryNotFoundException() throws Exception {
		mockMvc.perform(get("/test/diary/not-found").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/diary/not-found"))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("AccessDeniedException은 diary forbidden Problem Details를 반환한다")
	void handlesAccessDeniedException() throws Exception {
		mockMvc.perform(get("/test/diary/forbidden").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/diary/forbidden"))
				.andExpect(jsonPath("$.status").value(403));
	}

	@RestController
	static class TestController {
		@GetMapping("/test/diary/not-found")
		String notFound() {
			throw new DiaryNotFoundException();
		}

		@GetMapping("/test/diary/forbidden")
		String forbidden() {
			throw new AccessDeniedException("denied");
		}

		@GetMapping("/test/diary/conflict")
		String conflict() {
			throw new DuplicateDiaryException("already exists");
		}

		@GetMapping("/test/diary/entity-not-found")
		String entityNotFound() {
			throw new EntityNotFoundException("missing");
		}
	}
}
