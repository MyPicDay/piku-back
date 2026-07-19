package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.application.policy.FeedAuthorMaskingPolicy;
import com.pikume.back.feed.application.port.out.LoadFeedAuthorsPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryDetailPort;
import com.pikume.back.feed.application.port.out.LoadFeedEngagementPort;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedDetailQueryService")
class FeedDetailQueryServiceTest {

	@Mock
	private LoadFeedDiaryDetailPort loadFeedDiaryDetailPort;
	@Mock
	private LoadFeedAuthorsPort loadFeedAuthorsPort;
	@Mock
	private LoadFeedEngagementPort loadFeedEngagementPort;

	private FeedDetailQueryService service;

	@BeforeEach
	void setUp() {
		service = new FeedDetailQueryService(
				loadFeedDiaryDetailPort,
				loadFeedAuthorsPort,
				loadFeedEngagementPort,
				new FeedAuthorMaskingPolicy());
	}

	@Nested
	@DisplayName("queryDetail")
	class QueryDetail {

		@Test
		@DisplayName("조회 가능한 일기에 작성자와 반응 정보를 조합한다")
		void assemblesVisibleDiary() {
			FeedDiaryDetailView diary = detail(FeedVisibility.PUBLIC, "writer-id");
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "viewer-id"))
					.willReturn(Optional.of(diary));
			given(loadFeedAuthorsPort.loadAuthors(Set.of("writer-id")))
					.willReturn(Map.of(
							"writer-id",
							new FeedAuthorView("writer-id", "writer", "avatar-url")));
			given(loadFeedEngagementPort.loadEngagements("viewer-id", List.of(1L)))
					.willReturn(Map.of(1L, new FeedEngagementView(1L, 4L, 7L, true)));

			FeedDiaryResult result = service.queryDetail(1L, "viewer-id");

			assertThat(result.getDiaryId()).isEqualTo(1L);
			assertThat(result.getNickname()).isEqualTo("writer");
			assertThat(result.getAvatar()).isEqualTo("avatar-url");
			assertThat(result.getUserId()).isEqualTo("writer-id");
			assertThat(result.getFriendStatus()).isNull();
			assertThat(result.getCommentCount()).isEqualTo(4L);
			assertThat(result.getLikeCount()).isEqualTo(7L);
			assertThat(result.getIsLiked()).isTrue();
			assertThat(result.getIsOwner()).isFalse();
		}

		@Test
		@DisplayName("익명 일기는 작성자 정보를 조회하지 않고 마스킹한다")
		void masksAnonymousDiary() {
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "writer-id"))
					.willReturn(Optional.of(detail(FeedVisibility.ANONYMOUS, "writer-id")));
			given(loadFeedEngagementPort.loadEngagements("writer-id", List.of(1L)))
					.willReturn(Map.of());

			FeedDiaryResult result = service.queryDetail(1L, "writer-id");

			assertThat(result.getNickname()).isEqualTo("익명");
			assertThat(result.getAvatar()).isNull();
			assertThat(result.getUserId()).isNull();
			assertThat(result.getFriendStatus()).isEqualTo(FeedFriendStatus.ANONYMOUS);
			assertThat(result.getIsOwner()).isTrue();
			then(loadFeedAuthorsPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("조회할 수 없는 일기는 찾을 수 없음으로 처리한다")
		void rejectsInvisibleDiary() {
			given(loadFeedDiaryDetailPort.loadVisibleDiary(1L, "viewer-id"))
					.willReturn(Optional.empty());

			assertThatThrownBy(() -> service.queryDetail(1L, "viewer-id"))
					.isInstanceOf(FeedDiaryNotFoundException.class);
		}
	}

	private FeedDiaryDetailView detail(FeedVisibility visibility, String writerId) {
		return new FeedDiaryDetailView(
				1L,
				writerId,
				visibility,
				"content",
				List.of("photo-url"),
				LocalDate.of(2026, 3, 8),
				LocalDateTime.of(2026, 3, 8, 10, 0));
	}
}
