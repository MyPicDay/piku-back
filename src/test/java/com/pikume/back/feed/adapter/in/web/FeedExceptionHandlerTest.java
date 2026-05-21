package com.pikume.back.feed.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pikume.back.feed.adapter.in.web.problem.FeedProblemType;
import com.pikume.back.feed.application.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.application.exception.FeedErrorCode;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("FeedExceptionHandler")
class FeedExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(
						new GlobalExceptionHandler(java.util.Optional.empty(), problemDetailFactory),
						new FeedExceptionHandler(problemDetailFactory))
				.build();
	}

	@Test
	@DisplayName("FeedDiaryNotFoundException은 feed 전용 Problem Details를 반환한다")
	void handlesFeedDiaryNotFoundException() throws Exception {
		mockMvc.perform(get("/test/feed/not-found").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/feed/diary-not-found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.instance").value("/test/feed/not-found"));
	}

	@Test
	@DisplayName("InvalidFeedCursorException은 feed invalid-cursor Problem Details를 반환한다")
	void handlesInvalidFeedCursorException() throws Exception {
		mockMvc.perform(get("/test/feed/invalid-cursor").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(FeedProblemType.INVALID_CURSOR.type().toString()))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value(FeedErrorCode.INVALID_CURSOR.getMessage()))
				.andExpect(jsonPath("$.instance").value("/test/feed/invalid-cursor"));
	}

	@RestController
	static class TestController {
		@GetMapping("/test/feed/not-found")
		String notFound() {
			throw new FeedDiaryNotFoundException();
		}

		@GetMapping("/test/feed/invalid-cursor")
		String invalidCursor() {
			throw new InvalidFeedCursorException();
		}
	}
}
