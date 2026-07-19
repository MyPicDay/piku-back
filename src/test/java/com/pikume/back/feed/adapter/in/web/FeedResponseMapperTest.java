package com.pikume.back.feed.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.pikume.back.feed.adapter.in.web.dto.FeedCursorPageResponse;
import com.pikume.back.feed.adapter.in.web.dto.FeedDiaryResponse;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedResponseMapper")
class FeedResponseMapperTest {

	private final FeedResponseMapper mapper = new FeedResponseMapper();

	@Nested
	@DisplayName("mapDiary")
	class MapDiary {

		@Test
		@DisplayName("Application 일기 결과의 모든 필드를 Web 응답으로 변환한다")
		void mapsAllDiaryFields() {
			FeedDiaryResult result = diaryResult();

			FeedDiaryResponse response = mapper.mapDiary(result);

			assertThat(response.diaryId()).isEqualTo(10L);
			assertThat(response.status()).isEqualTo(FeedVisibility.PUBLIC);
			assertThat(response.content()).isEqualTo("feed-content");
			assertThat(response.imgUrls()).containsExactly("https://cdn.example/feed.jpg");
			assertThat(response.date()).isEqualTo(LocalDate.of(2026, 3, 8));
			assertThat(response.nickname()).isEqualTo("writer");
			assertThat(response.avatar()).isEqualTo("https://cdn.example/avatar.png");
			assertThat(response.userId()).isEqualTo("writer-id");
			assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 3, 8, 10, 0));
			assertThat(response.friendStatus()).isEqualTo(FeedFriendStatus.NONE);
			assertThat(response.commentCount()).isEqualTo(2L);
			assertThat(response.likeCount()).isEqualTo(5L);
			assertThat(response.isLiked()).isFalse();
			assertThat(response.isOwner()).isTrue();
		}
	}

	@Nested
	@DisplayName("mapPage")
	class MapPage {

		@Test
		@DisplayName("Application 페이지의 항목과 Cursor 정보를 Web 응답으로 변환한다")
		void mapsPage() {
			FeedCursorPage<FeedDiaryResult> result = new FeedCursorPage<>(
					List.of(diaryResult()),
					"opaque-next-cursor",
					true);

			FeedCursorPageResponse response = mapper.mapPage(result);

			assertThat(response.items()).extracting(FeedDiaryResponse::diaryId).containsExactly(10L);
			assertThat(response.nextCursor()).isEqualTo("opaque-next-cursor");
			assertThat(response.hasNext()).isTrue();
		}
	}

	private FeedDiaryResult diaryResult() {
		return FeedDiaryResult.builder()
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
				.isOwner(true)
				.build();
	}
}
