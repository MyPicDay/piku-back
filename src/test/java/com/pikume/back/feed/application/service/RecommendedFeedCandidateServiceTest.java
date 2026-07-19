package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.out.LoadFeedCandidateEngagementSignalsPort;
import com.pikume.back.feed.application.port.out.LoadFeedClickHistoryPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryCandidateSourcePort;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendedFeedCandidateService")
class RecommendedFeedCandidateServiceTest {

	@Mock
	private LoadFeedDiaryCandidateSourcePort loadFeedDiaryCandidateSourcePort;
	@Mock
	private LoadFeedFriendshipPort loadFeedFriendshipPort;
	@Mock
	private LoadFeedCandidateEngagementSignalsPort loadFeedCandidateEngagementSignalsPort;
	@Mock
	private LoadFeedClickHistoryPort loadFeedClickHistoryPort;

	private RecommendedFeedCandidateService service;

	@BeforeEach
	void setUp() {
		service = new RecommendedFeedCandidateService(
				loadFeedDiaryCandidateSourcePort,
				loadFeedFriendshipPort,
				loadFeedCandidateEngagementSignalsPort,
				loadFeedClickHistoryPort);
	}

	@Nested
	@DisplayName("loadCandidates")
	class LoadCandidates {

		@Test
		@DisplayName("친구 Bucket에서 소비하지 않은 후보를 기존 Comparator 순서로 반환한다")
		void loadsNotConsumedFriendCandidatesInOrder() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 12, 0);
			given(loadFeedFriendshipPort.loadFriendUserIds("viewer"))
					.willReturn(List.of("friend"));
			given(loadFeedDiaryCandidateSourcePort.loadRecentDiaryIdsByVisibilityAndAuthors(
					FeedVisibility.FRIENDS, List.of("friend"), 50))
					.willReturn(List.of(1L, 2L));
			given(loadFeedDiaryCandidateSourcePort.loadRecentDiaryIdsByVisibilityAndAuthors(
					FeedVisibility.PUBLIC, List.of("friend"), 50))
					.willReturn(List.of(3L));
			given(loadFeedDiaryCandidateSourcePort.loadCandidateAttributes(Set.of(1L, 2L, 3L)))
					.willReturn(Map.of(
							1L, new FeedDiaryCandidateView(1L, "friend", now.minusHours(1)),
							2L, new FeedDiaryCandidateView(2L, "friend", now.minusHours(2)),
							3L, new FeedDiaryCandidateView(3L, "friend", now.minusHours(1))));
			given(loadFeedClickHistoryPort.loadClickedDiaryIds(
					"viewer", List.of(1L, 2L, 3L)))
					.willReturn(Set.of(2L));
			given(loadFeedCandidateEngagementSignalsPort.loadLikedDiaryIds("viewer", List.of(1L, 2L, 3L)))
					.willReturn(Set.of());
			given(loadFeedCandidateEngagementSignalsPort.loadCommentedDiaryIds("viewer", List.of(1L, 2L, 3L)))
					.willReturn(Set.of());
			given(loadFeedCandidateEngagementSignalsPort.loadLikeCounts(List.of(1L, 2L, 3L)))
					.willReturn(Map.of(1L, 1L, 3L, 5L));
			given(loadFeedCandidateEngagementSignalsPort.loadCommentCounts(List.of(1L, 2L, 3L)))
					.willReturn(Map.of(1L, 2L, 3L, 1L));

			List<FeedCursorCandidate> result = service.loadCandidates(
					"viewer",
					FeedBucket.NOT_CONSUMED_FRIEND,
					null,
					2);

			assertThat(result).extracting(FeedCursorCandidate::diaryId).containsExactly(3L, 1L);
		}

		@Test
		@DisplayName("공개 Bucket은 친구의 공개 일기를 제외하고 익명 일기는 유지한다")
		void excludesFriendPublicDiaryButKeepsAnonymousDiary() {
			LocalDateTime now = LocalDateTime.of(2026, 3, 8, 12, 0);
			given(loadFeedFriendshipPort.loadFriendUserIds("viewer"))
					.willReturn(List.of("friend"));
			given(loadFeedDiaryCandidateSourcePort.loadRecentDiaryIdsByVisibilityExcludingAuthor(
					FeedVisibility.PUBLIC, "viewer", 50))
					.willReturn(List.of(1L, 2L));
			given(loadFeedDiaryCandidateSourcePort.loadRecentDiaryIdsByVisibilityExcludingAuthor(
					FeedVisibility.ANONYMOUS, "viewer", 50))
					.willReturn(List.of(3L));
			given(loadFeedDiaryCandidateSourcePort.loadCandidateAttributes(Set.of(1L, 2L)))
					.willReturn(Map.of(
							1L, new FeedDiaryCandidateView(1L, "friend", now),
							2L, new FeedDiaryCandidateView(2L, "other", now.minusMinutes(1))));
			given(loadFeedDiaryCandidateSourcePort.loadCandidateAttributes(Set.of(2L, 3L)))
					.willReturn(Map.of(
							2L, new FeedDiaryCandidateView(2L, "other", now.minusMinutes(1)),
							3L, new FeedDiaryCandidateView(3L, "friend", now.minusMinutes(2))));
			given(loadFeedClickHistoryPort.loadClickedDiaryIds("viewer", List.of(2L, 3L)))
					.willReturn(Set.of());
			given(loadFeedCandidateEngagementSignalsPort.loadLikedDiaryIds("viewer", List.of(2L, 3L)))
					.willReturn(Set.of());
			given(loadFeedCandidateEngagementSignalsPort.loadCommentedDiaryIds("viewer", List.of(2L, 3L)))
					.willReturn(Set.of());
			given(loadFeedCandidateEngagementSignalsPort.loadLikeCounts(List.of(2L, 3L))).willReturn(Map.of());
			given(loadFeedCandidateEngagementSignalsPort.loadCommentCounts(List.of(2L, 3L))).willReturn(Map.of());

			List<FeedCursorCandidate> result = service.loadCandidates(
					"viewer",
					FeedBucket.NOT_CONSUMED_PUBLIC,
					null,
					2);

			assertThat(result).extracting(FeedCursorCandidate::diaryId).containsExactly(2L, 3L);
		}

		@Test
		@DisplayName("익명 공개 Bucket 조회는 작성자 제외 조회를 사용하지 않는다")
		void loadsAnonymousPublicBucketWithoutAuthorExclusion() {
			LocalDateTime createdAt = LocalDateTime.of(2026, 3, 8, 12, 0);
			given(loadFeedDiaryCandidateSourcePort.loadRecentDiaryIdsByVisibility(
					FeedVisibility.PUBLIC, 50))
					.willReturn(List.of(1L));
			given(loadFeedDiaryCandidateSourcePort.loadRecentDiaryIdsByVisibility(
					FeedVisibility.ANONYMOUS, 50))
					.willReturn(List.of());
			given(loadFeedDiaryCandidateSourcePort.loadCandidateAttributes(Set.of(1L)))
					.willReturn(Map.of(1L, new FeedDiaryCandidateView(1L, "writer", createdAt)));

			List<FeedCursorCandidate> result = service.loadCandidates(
					null,
					FeedBucket.NOT_CONSUMED_PUBLIC,
					null,
					1);

			assertThat(result).extracting(FeedCursorCandidate::diaryId).containsExactly(1L);
			then(loadFeedDiaryCandidateSourcePort).should()
					.loadRecentDiaryIdsByVisibility(FeedVisibility.PUBLIC, 50);
			then(loadFeedDiaryCandidateSourcePort).should()
					.loadRecentDiaryIdsByVisibility(FeedVisibility.ANONYMOUS, 50);
			then(loadFeedDiaryCandidateSourcePort).should(never())
					.loadRecentDiaryIdsByVisibilityExcludingAuthor(
							org.mockito.ArgumentMatchers.any(),
							org.mockito.ArgumentMatchers.nullable(String.class),
							org.mockito.ArgumentMatchers.anyInt());
		}

		@Test
		@DisplayName("요청 수가 0 이하면 Provider를 호출하지 않는다")
		void returnsEmptyForNonPositiveLimit() {
			assertThat(service.loadCandidates("viewer", FeedBucket.NOT_CONSUMED_PUBLIC, null, 0))
					.isEmpty();

			then(loadFeedDiaryCandidateSourcePort).shouldHaveNoInteractions();
			then(loadFeedFriendshipPort).shouldHaveNoInteractions();
			then(loadFeedClickHistoryPort).should(never())
					.loadClickedDiaryIds(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
		}
	}
}
