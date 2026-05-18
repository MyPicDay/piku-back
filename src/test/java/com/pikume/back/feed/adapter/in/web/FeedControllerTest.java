package com.pikume.back.feed.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.in.GetFeedUseCase;
import com.pikume.back.feed.domain.exception.FeedDiaryNotFoundException;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.global.util.RequestMetaMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedController")
class FeedControllerTest {

	@InjectMocks
	private FeedController feedController;

	@Mock
	private GetFeedUseCase getFeedUseCase;

	@Mock
	private RequestMetaMapper requestMetaMapper;

	private MockMvc mockMvc;
	private RequestMetaInfo requestMetaInfo;
	private final ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(feedController)
				.setControllerAdvice(
						new GlobalExceptionHandler(java.util.Optional.empty(), problemDetailFactory),
						new FeedExceptionHandler(problemDetailFactory))
				.build();
		requestMetaInfo = new RequestMetaInfo(
				"https",
				"localhost",
				8080,
				"localhost:8080",
				"https://localhost:8080/api/diary",
				"JUnit",
				"127.0.0.1");
	}

	@Test
	@DisplayName("GET /api/diary는 cursor와 limit 계약으로 피드 목록을 반환한다")
	void getAllDiariesReturnsCursorPage() throws Exception {
		FeedCursorPage<FeedDiaryResult> page = new FeedCursorPage<>(
				List.of(FeedDiaryResult.builder()
						.diaryId(10L)
						.status(FeedVisibility.PUBLIC)
						.content("feed-content")
						.imgUrls(List.of("https://cdn.example/feed.jpg"))
						.date(LocalDate.of(2026, 3, 8))
						.nickname("writer")
						.avatar("https://cdn.example/avatar.png")
						.userId("writer-id")
						.createdAt(LocalDateTime.of(2026, 3, 8, 10, 0))
						.friendStatus(FeedFriendStatus.NONE)
						.commentCount(2L)
						.likeCount(5L)
						.isLiked(false)
						.build()),
				"opaque-next-cursor",
				true);

		given(requestMetaMapper.extractMetaInfo(any(HttpServletRequest.class))).willReturn(requestMetaInfo);
		given(getFeedUseCase.getAllDiaries(new FeedCursorRequest("cursor-token", 10), requestMetaInfo, null))
				.willReturn(page);

		mockMvc.perform(get("/api/diary")
						.param("cursor", "cursor-token")
						.param("limit", "10")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].diaryId").value(10L))
				.andExpect(jsonPath("$.items[0].content").value("feed-content"))
				.andExpect(jsonPath("$.nextCursor").value("opaque-next-cursor"))
				.andExpect(jsonPath("$.hasNext").value(true));

		verify(getFeedUseCase).getAllDiaries(new FeedCursorRequest("cursor-token", 10), requestMetaInfo, null);
	}

	@Test
	@DisplayName("GET /api/diary/{diaryId}는 비공개 일기 접근 시 404를 반환한다")
	void getDiaryWithPhotosReturnsNotFoundWhenDiaryIsHidden() throws Exception {
		given(requestMetaMapper.extractMetaInfo(any(HttpServletRequest.class))).willReturn(requestMetaInfo);
		given(getFeedUseCase.getDiaryWithPhotos(1L, requestMetaInfo, null))
				.willThrow(new FeedDiaryNotFoundException());

		mockMvc.perform(get("/api/diary/1")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}
}
