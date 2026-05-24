package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
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
import com.pikume.back.feed.application.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.application.exception.FeedErrorCode;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.feed.application.port.out.LoadFeedClickPort;
import com.pikume.back.feed.application.port.out.LoadFeedCursorCandidatesPort;
import com.pikume.back.feed.application.port.out.LoadFeedListViewPort;
import com.pikume.back.feed.application.port.out.LoadLatestFeedCandidatesPort;
import com.pikume.back.feed.application.port.out.LoadRecommendationForFeedPort;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;
import com.pikume.back.feed.application.port.out.LoadUserForFeedPort;
import com.pikume.back.feed.application.port.out.SaveFeedClickPort;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedQueryService")
class FeedQueryServiceTest {

	@InjectMocks
	private FeedQueryService feedQueryService;

	@Mock
	private LoadDiaryForFeedPort loadDiaryForFeedPort;
	@Mock
	private LoadFeedListViewPort loadFeedListViewPort;
	@Mock
	private LoadFeedCursorCandidatesPort loadFeedCursorCandidatesPort;
	@Mock
	private LoadLatestFeedCandidatesPort loadLatestFeedCandidatesPort;
	@Mock
	private LoadSocialForFeedPort loadSocialForFeedPort;
	@Mock
	private LoadUserForFeedPort loadUserForFeedPort;
	@Mock
	private LoadRecommendationForFeedPort loadRecommendationForFeedPort;
	@Mock
	private LoadFeedClickPort loadFeedClickPort;
	@Mock
	private SaveFeedClickPort saveFeedClickPort;
	@Mock
	private FeedCursorTokenCodec feedCursorTokenCodec;

	private Diary publicDiary;
	private Diary friendsDiary;
	private Diary privateDiary;
	private RequestMetaInfo requestMetaInfo;

	@BeforeEach
	void setUp() {
		publicDiary = new Diary("공개 일기 내용", DiaryVisibility.PUBLIC, LocalDate.now(), "owner-id");
		friendsDiary = new Diary("친구 공개 일기", DiaryVisibility.FRIENDS, LocalDate.now(), "owner-id");
		privateDiary = new Diary("비공개 일기", DiaryVisibility.PRIVATE, LocalDate.now(), "owner-id");

		requestMetaInfo = new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
				"https://localhost:8080/api/diary", "TestAgent", "127.0.0.1");
	}

	@Nested
	@DisplayName("getDiaryWithPhotos - 단일 일기 상세 조회")
	class GetDiaryWithPhotos {

		@Test
		@DisplayName("공개 일기는 누구나 전체 내용을 조회할 수 있다")
		void publicDiaryAccessibleByAnyone() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "viewer-id"))
					.willReturn(java.util.Optional.of(detailView(publicDiary, List.of("photo1.jpg", "photo2.jpg"))));
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(10L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(false);
			given(loadSocialForFeedPort.countComments(eq("viewer-id"), any())).willReturn(5L);

			FeedDiaryResult result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isEqualTo("공개 일기 내용");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getLikeCount()).isEqualTo(10L);
		}

		@Test
		@DisplayName("비공개 일기는 본인만 전체 내용을 조회할 수 있다")
		void privateDiaryAccessibleByOwner() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "owner-id"))
					.willReturn(java.util.Optional.of(detailView(privateDiary, List.of("photo1.jpg"))));
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(0L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(false);
			given(loadSocialForFeedPort.countComments(eq("owner-id"), any())).willReturn(0L);

			FeedDiaryResult result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "owner-id");

			assertThat(result.getContent()).isEqualTo("비공개 일기");
		}

		@Test
		@DisplayName("비공개 일기는 타인에게 존재가 노출되지 않는다")
		void privateDiaryHiddenFromOthers() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "viewer-id")).willReturn(java.util.Optional.empty());

				assertThatThrownBy(() -> feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id"))
						.isInstanceOfSatisfying(FeedDiaryNotFoundException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(FeedErrorCode.DIARY_NOT_FOUND));
		}

		@Test
		@DisplayName("친구 공개 일기는 친구에게 전체 내용이 보인다")
		void friendsDiaryAccessibleByFriend() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "friend-id"))
					.willReturn(java.util.Optional.of(detailView(friendsDiary, List.of("photo1.jpg", "photo2.jpg"))));
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(5L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(true);
			given(loadSocialForFeedPort.countComments(eq("friend-id"), any())).willReturn(3L);

			FeedDiaryResult result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "friend-id");

			assertThat(result.getContent()).isEqualTo("친구 공개 일기");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getIsLiked()).isTrue();
		}

		@Test
		@DisplayName("친구 공개 일기는 비친구에게 존재가 노출되지 않는다")
		void friendsDiaryHiddenFromStranger() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "stranger-id")).willReturn(java.util.Optional.empty());

				assertThatThrownBy(() -> feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "stranger-id"))
						.isInstanceOfSatisfying(FeedDiaryNotFoundException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(FeedErrorCode.DIARY_NOT_FOUND));
		}
	}

	@Nested
	@DisplayName("getAllDiaries - cursor 기반 피드 목록 조회")
	class GetAllDiaries {

		@Test
		@DisplayName("첫 페이지 조회 시 현재 bucket에서 limit만큼 읽고 nextCursor를 반환한다")
		void firstPageUsesCurrentBucketAndReturnsNextCursor() {
			FeedCursorCandidate first = candidate(FeedBucket.NOT_CONSUMED_FRIEND, 30L, 5L, 2L);
			FeedCursorCandidate second = candidate(FeedBucket.NOT_CONSUMED_FRIEND, 20L, 4L, 1L);

			given(loadFeedCursorCandidatesPort.loadCandidates("viewer-id", FeedBucket.NOT_CONSUMED_FRIEND, null, 2))
					.willReturn(List.of(first, second));
			given(loadFeedCursorCandidatesPort.loadCandidates("viewer-id", FeedBucket.NOT_CONSUMED_FRIEND, second.toCursor(), 1))
					.willReturn(List.of(candidate(FeedBucket.NOT_CONSUMED_FRIEND, 10L, 3L, 1L)));
			given(loadFeedListViewPort.loadFeedListItems(List.of(30L, 20L), "viewer-id")).willReturn(List.of(
					feedItem(30L, "writer-30", FeedFriendStatus.FRIENDS),
					feedItem(20L, "writer-20", FeedFriendStatus.FRIENDS)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo)))
					.willAnswer(invocation -> invocation.getArgument(0));
			given(feedCursorTokenCodec.encode(second.toCursor())).willReturn("next-token");

			FeedCursorPage<FeedDiaryResult> result = feedQueryService.getAllDiaries(
					new FeedCursorRequest(null, 2),
					requestMetaInfo,
					"viewer-id");

			assertThat(result.items()).extracting(FeedDiaryResult::getDiaryId).containsExactly(30L, 20L);
			assertThat(result.hasNext()).isTrue();
			assertThat(result.nextCursor()).isEqualTo("next-token");
			verify(loadLatestFeedCandidatesPort, never()).loadCandidates(anyString(), any(), any(), anyInt());
		}

		@Test
		@DisplayName("현재 bucket이 부족하면 다음 bucket으로 넘어가서 채운다")
		void movesToNextBucketWhenCurrentBucketIsExhausted() {
			FeedCursorCandidate friendDiary = candidate(FeedBucket.NOT_CONSUMED_FRIEND, 30L, 5L, 0L);
			FeedCursorCandidate publicDiary = candidate(FeedBucket.NOT_CONSUMED_PUBLIC, 10L, 2L, 3L);

			given(loadFeedCursorCandidatesPort.loadCandidates("viewer-id", FeedBucket.NOT_CONSUMED_FRIEND, null, 2))
					.willReturn(List.of(friendDiary));
			given(loadFeedCursorCandidatesPort.loadCandidates("viewer-id", FeedBucket.NOT_CONSUMED_PUBLIC, null, 1))
					.willReturn(List.of(publicDiary));
			given(loadFeedCursorCandidatesPort.loadCandidates("viewer-id", FeedBucket.NOT_CONSUMED_PUBLIC, publicDiary.toCursor(), 1))
					.willReturn(List.of(candidate(FeedBucket.NOT_CONSUMED_PUBLIC, 9L, 1L, 1L)));
			given(loadFeedListViewPort.loadFeedListItems(List.of(30L, 10L), "viewer-id")).willReturn(List.of(
					feedItem(30L, "friend-writer", FeedFriendStatus.FRIENDS),
					feedItem(10L, "public-writer", FeedFriendStatus.NONE)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo)))
					.willAnswer(invocation -> invocation.getArgument(0));
			given(feedCursorTokenCodec.encode(publicDiary.toCursor())).willReturn("next-public-token");

			FeedCursorPage<FeedDiaryResult> result = feedQueryService.getAllDiaries(
					new FeedCursorRequest(null, 2),
					requestMetaInfo,
					"viewer-id");

			assertThat(result.items()).extracting(FeedDiaryResult::getDiaryId).containsExactly(30L, 10L);
			assertThat(result.hasNext()).isTrue();
			assertThat(result.nextCursor()).isEqualTo("next-public-token");
		}

		@Test
		@DisplayName("유효하지 않은 cursor bucket은 예외를 던진다")
		void invalidCursorBucketThrowsException() {
			FeedCursor invalidCursor = new FeedCursor(
					FeedBucket.CONSUMED_PUBLIC,
					1L,
					0L,
					LocalDateTime.now(),
					99L);
			given(feedCursorTokenCodec.decode("invalid-token")).willReturn(invalidCursor);

				assertThatThrownBy(() -> feedQueryService.getAllDiaries(
						new FeedCursorRequest("invalid-token", 20),
						requestMetaInfo,
						null))
						.isInstanceOfSatisfying(InvalidFeedCursorException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(FeedErrorCode.INVALID_CURSOR));
		}

		@Test
		@DisplayName("최신순 모드는 latest 후보 경로를 호출하고 materialization 순서를 보존한다")
		void latestSortUsesLatestCandidatePathAndPreservesOrder() {
			FeedLatestCursorCandidate first = latestCandidate(90L);
			FeedLatestCursorCandidate second = latestCandidate(80L);
			FeedLatestCursorCandidate next = latestCandidate(70L);

			given(loadSocialForFeedPort.getFriendIds("viewer-id")).willReturn(List.of("friend-a"));
			given(loadLatestFeedCandidatesPort.loadCandidates("viewer-id", List.of("friend-a"), null, 2))
					.willReturn(List.of(first, second));
			given(loadLatestFeedCandidatesPort.loadCandidates("viewer-id", List.of("friend-a"), second.toCursor(), 1))
					.willReturn(List.of(next));
			given(loadFeedListViewPort.loadFeedListItems(List.of(90L, 80L), "viewer-id")).willReturn(List.of(
					feedItem(90L, "public-writer", FeedFriendStatus.NONE),
					feedItem(80L, "friend-writer", FeedFriendStatus.FRIENDS)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo)))
					.willAnswer(invocation -> invocation.getArgument(0));
			given(feedCursorTokenCodec.encode(second.toCursor())).willReturn("latest-next-token");

			FeedCursorPage<FeedDiaryResult> result = feedQueryService.getAllDiaries(
					new FeedCursorRequest(null, 2, FeedSortMode.LATEST),
					requestMetaInfo,
					"viewer-id");

			assertThat(result.items()).extracting(FeedDiaryResult::getDiaryId).containsExactly(90L, 80L);
			assertThat(result.hasNext()).isTrue();
			assertThat(result.nextCursor()).isEqualTo("latest-next-token");
			verify(loadFeedCursorCandidatesPort, never()).loadCandidates(anyString(), any(), any(), anyInt());
		}

		@Test
		@DisplayName("정렬 모드 metadata가 없는 legacy cursor는 최신순 모드에서 거부된다")
		void legacyCursorIsRejectedForLatestSort() {
			FeedCursor legacyCursor = new FeedCursor(
					FeedBucket.NOT_CONSUMED_PUBLIC,
					1L,
					0L,
					LocalDateTime.now(),
					99L);
			given(feedCursorTokenCodec.decode("legacy-token")).willReturn(legacyCursor);

				assertThatThrownBy(() -> feedQueryService.getAllDiaries(
						new FeedCursorRequest("legacy-token", 20, FeedSortMode.LATEST),
						requestMetaInfo,
						"viewer-id"))
						.isInstanceOfSatisfying(InvalidFeedCursorException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(FeedErrorCode.INVALID_CURSOR));
		}

		@Test
		@DisplayName("최신순 cursor는 추천순 모드에서 거부된다")
		void latestCursorIsRejectedForRecommendedSort() {
			FeedCursor latestCursor = FeedCursor.latest(LocalDateTime.now(), 99L);
			given(feedCursorTokenCodec.decode("latest-token")).willReturn(latestCursor);

				assertThatThrownBy(() -> feedQueryService.getAllDiaries(
						new FeedCursorRequest("latest-token", 20),
						requestMetaInfo,
						"viewer-id"))
						.isInstanceOfSatisfying(InvalidFeedCursorException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(FeedErrorCode.INVALID_CURSOR));
		}

		private FeedCursorCandidate candidate(FeedBucket bucket, Long diaryId, long likeCount, long commentCount) {
			return new FeedCursorCandidate(bucket, diaryId, likeCount, commentCount, LocalDateTime.now().minusDays(diaryId));
		}

		private FeedLatestCursorCandidate latestCandidate(Long diaryId) {
			return new FeedLatestCursorCandidate(diaryId, LocalDateTime.now().minusDays(diaryId));
		}

		private FeedListItemView feedItem(Long diaryId, String writerId, FeedFriendStatus friendStatus) {
			return new FeedListItemView(
					diaryId,
					FeedVisibility.PUBLIC,
					"content-" + diaryId,
					List.of("photo-" + diaryId + ".jpg"),
					LocalDate.now(),
					"nick-" + writerId,
					"avatar-" + writerId + ".png",
					writerId,
					LocalDateTime.now(),
					friendStatus,
					2L,
					3L,
					true);
		}
	}

	@Nested
	@DisplayName("logClick - 클릭 로그 기록")
	class LogClick {

		@Test
		@DisplayName("처음 클릭하면 로그가 저장된다")
		void firstClickSavesLog() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "user-id"))
					.willReturn(java.util.Optional.of(detailView(publicDiary, List.of())));
			given(loadFeedClickPort.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(false);

			feedQueryService.logClick("user-id", 1L);

			verify(saveFeedClickPort).save(any(FeedClick.class));
		}

		@Test
		@DisplayName("이미 클릭한 경우 중복 저장하지 않는다")
		void duplicateClickIsIgnored() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "user-id"))
					.willReturn(java.util.Optional.of(detailView(publicDiary, List.of())));
			given(loadFeedClickPort.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(true);

			feedQueryService.logClick("user-id", 1L);

			verify(saveFeedClickPort, never()).save(any());
		}

		@Test
		@DisplayName("숨겨진 일기는 클릭 로그를 남기지 않는다")
		void hiddenDiaryDoesNotRecordClick() {
			given(loadDiaryForFeedPort.findVisibleDiaryById(1L, "stranger-id")).willReturn(java.util.Optional.empty());

			feedQueryService.logClick("stranger-id", 1L);

			verify(loadFeedClickPort, never()).existsByUserIdAndDiaryId(anyString(), any());
			verify(saveFeedClickPort, never()).save(any());
		}
	}

	private FeedDiaryDetailView detailView(Diary diary, List<String> imageUrls) {
		return new FeedDiaryDetailView(
				1L,
				diary.getUserId(),
				FeedVisibility.valueOf(diary.getStatus().name()),
				diary.getContent(),
				imageUrls,
				diary.getDate(),
				diary.getCreatedAt());
	}
}
