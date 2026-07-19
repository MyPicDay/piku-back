package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedLatestCursorCandidate;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.port.out.LoadLatestFeedCandidatesPort;
import com.pikume.back.feed.application.readmodel.FeedListItemView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedPageQueryService")
class FeedPageQueryServiceTest {

	@InjectMocks
	private FeedPageQueryService service;

	@Mock
	private FeedListItemAssembler feedListItemAssembler;
	@Mock
	private RecommendedFeedCandidateService recommendedFeedCandidateService;
	@Mock
	private LoadLatestFeedCandidatesPort loadLatestFeedCandidatesPort;
	@Mock
	private LoadFeedFriendshipPort loadFeedFriendshipPort;
	@Mock
	private FeedCursorTokenCodec feedCursorTokenCodec;

	@Nested
	@DisplayName("queryPage")
	class QueryPage {

		@Test
		@DisplayName("추천순 첫 페이지는 현재 Bucket 후보 순서를 보존하고 다음 Cursor를 반환한다")
		void returnsRecommendedPageInCandidateOrder() {
			FeedCursorCandidate first = candidate(FeedBucket.NOT_CONSUMED_FRIEND, 30L);
			FeedCursorCandidate second = candidate(FeedBucket.NOT_CONSUMED_FRIEND, 20L);
			given(recommendedFeedCandidateService.loadCandidates(
					"viewer-id", FeedBucket.NOT_CONSUMED_FRIEND, null, 2))
					.willReturn(List.of(first, second));
			given(recommendedFeedCandidateService.loadCandidates(
					"viewer-id", FeedBucket.NOT_CONSUMED_FRIEND, second.toCursor(), 1))
					.willReturn(List.of(candidate(FeedBucket.NOT_CONSUMED_FRIEND, 10L)));
			given(feedListItemAssembler.assemble(List.of(30L, 20L), "viewer-id"))
					.willReturn(List.of(
							feedItem(30L, "writer-30", FeedFriendStatus.FRIENDS),
							feedItem(20L, "writer-20", FeedFriendStatus.FRIENDS)));
			given(feedCursorTokenCodec.encode(second.toCursor())).willReturn("next-token");

			FeedCursorPage<FeedDiaryResult> result = service.queryPage(
					new FeedCursorRequest(null, 2),
					"viewer-id");

			assertThat(result.items()).extracting(FeedDiaryResult::getDiaryId)
					.containsExactly(30L, 20L);
			assertThat(result.nextCursor()).isEqualTo("next-token");
			assertThat(result.hasNext()).isTrue();
			verify(loadLatestFeedCandidatesPort, never())
					.loadCandidates(anyString(), any(), any(), anyInt());
		}

		@Test
		@DisplayName("현재 추천 Bucket이 부족하면 다음 Bucket 후보로 페이지를 채운다")
		void movesToNextRecommendedBucket() {
			FeedCursorCandidate friend = candidate(FeedBucket.NOT_CONSUMED_FRIEND, 30L);
			FeedCursorCandidate publicDiary = candidate(FeedBucket.NOT_CONSUMED_PUBLIC, 10L);
			given(recommendedFeedCandidateService.loadCandidates(
					"viewer-id", FeedBucket.NOT_CONSUMED_FRIEND, null, 2))
					.willReturn(List.of(friend));
			given(recommendedFeedCandidateService.loadCandidates(
					"viewer-id", FeedBucket.NOT_CONSUMED_PUBLIC, null, 1))
					.willReturn(List.of(publicDiary));
			given(recommendedFeedCandidateService.loadCandidates(
					"viewer-id", FeedBucket.NOT_CONSUMED_PUBLIC, publicDiary.toCursor(), 1))
					.willReturn(List.of());
			given(recommendedFeedCandidateService.loadCandidates(
					"viewer-id", FeedBucket.CONSUMED_FRIEND, null, 1))
					.willReturn(List.of(candidate(FeedBucket.CONSUMED_FRIEND, 5L)));
			given(feedListItemAssembler.assemble(List.of(30L, 10L), "viewer-id"))
					.willReturn(List.of(
							feedItem(30L, "friend", FeedFriendStatus.FRIENDS),
							feedItem(10L, "public", FeedFriendStatus.NONE)));
			given(feedCursorTokenCodec.encode(publicDiary.toCursor())).willReturn("next-bucket");

			FeedCursorPage<FeedDiaryResult> result = service.queryPage(
					new FeedCursorRequest(null, 2),
					"viewer-id");

			assertThat(result.items()).extracting(FeedDiaryResult::getDiaryId)
					.containsExactly(30L, 10L);
			assertThat(result.hasNext()).isTrue();
		}

		@Test
		@DisplayName("최신순은 친구 ID와 최신순 후보 Port를 사용하고 후보 순서를 보존한다")
		void returnsLatestPageInCandidateOrder() {
			FeedLatestCursorCandidate first = latestCandidate(90L);
			FeedLatestCursorCandidate second = latestCandidate(80L);
			given(loadFeedFriendshipPort.loadFriendUserIds("viewer-id"))
					.willReturn(List.of("friend-a"));
			given(loadLatestFeedCandidatesPort.loadCandidates(
					"viewer-id", List.of("friend-a"), null, 2))
					.willReturn(List.of(first, second));
			given(loadLatestFeedCandidatesPort.loadCandidates(
					"viewer-id", List.of("friend-a"), second.toCursor(), 1))
					.willReturn(List.of(latestCandidate(70L)));
			given(feedListItemAssembler.assemble(List.of(90L, 80L), "viewer-id"))
					.willReturn(List.of(
							feedItem(90L, "public", FeedFriendStatus.NONE),
							feedItem(80L, "friend", FeedFriendStatus.FRIENDS)));
			given(feedCursorTokenCodec.encode(second.toCursor())).willReturn("latest-next");

			FeedCursorPage<FeedDiaryResult> result = service.queryPage(
					new FeedCursorRequest(null, 2, FeedSortMode.LATEST),
					"viewer-id");

			assertThat(result.items()).extracting(FeedDiaryResult::getDiaryId)
					.containsExactly(90L, 80L);
			assertThat(result.nextCursor()).isEqualTo("latest-next");
			verify(recommendedFeedCandidateService, never())
					.loadCandidates(anyString(), any(), any(), anyInt());
		}

		@Test
		@DisplayName("정렬 모드가 없는 기존 추천 Cursor는 최신순 요청에서 거부한다")
		void rejectsLegacyRecommendedCursorForLatestSort() {
			FeedCursor legacyCursor = new FeedCursor(
					FeedBucket.NOT_CONSUMED_PUBLIC,
					1L,
					0L,
					LocalDateTime.now(),
					99L);
			given(feedCursorTokenCodec.decode("legacy-token")).willReturn(legacyCursor);

			assertThatThrownBy(() -> service.queryPage(
					new FeedCursorRequest("legacy-token", 20, FeedSortMode.LATEST),
					"viewer-id"))
					.isInstanceOf(InvalidFeedCursorException.class);
		}

		@Test
		@DisplayName("최신순 Cursor는 추천순 요청에서 거부한다")
		void rejectsLatestCursorForRecommendedSort() {
			given(feedCursorTokenCodec.decode("latest-token"))
					.willReturn(FeedCursor.latest(LocalDate.now(), 99L));

			assertThatThrownBy(() -> service.queryPage(
					new FeedCursorRequest("latest-token", 20),
					"viewer-id"))
					.isInstanceOf(InvalidFeedCursorException.class);
		}
	}

	private FeedCursorCandidate candidate(FeedBucket bucket, Long diaryId) {
		return new FeedCursorCandidate(
				bucket,
				diaryId,
				diaryId,
				diaryId,
				LocalDateTime.of(2026, 3, 8, 12, 0).minusMinutes(diaryId));
	}

	private FeedLatestCursorCandidate latestCandidate(Long diaryId) {
		return new FeedLatestCursorCandidate(
				diaryId,
				LocalDate.of(2026, 3, 8).minusDays(diaryId));
	}

	private FeedListItemView feedItem(Long diaryId, String writerId, FeedFriendStatus friendStatus) {
		return new FeedListItemView(
				diaryId,
				FeedVisibility.PUBLIC,
				"content-" + diaryId,
				List.of("photo-" + diaryId + ".jpg"),
				LocalDate.of(2026, 3, 8),
				"nick-" + writerId,
				"avatar-" + writerId + ".png",
				writerId,
				LocalDateTime.of(2026, 3, 8, 10, 0),
				friendStatus,
				2L,
				3L,
				true,
				false);
	}
}
