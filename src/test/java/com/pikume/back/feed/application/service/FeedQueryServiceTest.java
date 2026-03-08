package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.diary.adapter.in.web.dto.ResponseDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.feed.application.port.out.LoadFeedClickPort;
import com.pikume.back.feed.application.port.out.LoadFeedCursorCandidatesPort;
import com.pikume.back.feed.application.port.out.LoadFeedListViewPort;
import com.pikume.back.feed.application.port.out.LoadRecommendationForFeedPort;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;
import com.pikume.back.feed.application.port.out.LoadUserForFeedPort;
import com.pikume.back.feed.application.port.out.SaveFeedClickPort;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.feed.domain.exception.InvalidFeedCursorException;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
			given(loadDiaryForFeedPort.getDiaryById(1L)).willReturn(publicDiary);
			given(loadDiaryForFeedPort.getPhotosForDiary(any(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(loadSocialForFeedPort.areFriends(anyString(), anyString())).willReturn(false);
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(10L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(false);
			given(loadSocialForFeedPort.countComments(any())).willReturn(5L);

			ResponseDTO result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isEqualTo("공개 일기 내용");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getLikeCount()).isEqualTo(10L);
		}

		@Test
		@DisplayName("비공개 일기는 본인만 전체 내용을 조회할 수 있다")
		void privateDiaryAccessibleByOwner() {
			given(loadDiaryForFeedPort.getDiaryById(1L)).willReturn(privateDiary);
			given(loadDiaryForFeedPort.getPhotosForDiary(any(), any())).willReturn(List.of("photo1.jpg"));
			given(loadSocialForFeedPort.areFriends(anyString(), anyString())).willReturn(false);
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(0L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(false);
			given(loadSocialForFeedPort.countComments(any())).willReturn(0L);

			ResponseDTO result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "owner-id");

			assertThat(result.getContent()).isEqualTo("비공개 일기");
		}

		@Test
		@DisplayName("비공개 일기는 타인에게 대표 사진만 보인다")
		void privateDiaryShowsOnlyThumbnailToOthers() {
			given(loadDiaryForFeedPort.getDiaryById(1L)).willReturn(privateDiary);
			given(loadDiaryForFeedPort.getPhotosForDiary(any(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(loadSocialForFeedPort.areFriends(anyString(), anyString())).willReturn(false);
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(0L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(false);
			given(loadSocialForFeedPort.countComments(any())).willReturn(0L);

			ResponseDTO result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isNull();
			assertThat(result.getImgUrls()).hasSize(1);
		}

		@Test
		@DisplayName("친구 공개 일기는 친구에게 전체 내용이 보인다")
		void friendsDiaryAccessibleByFriend() {
			given(loadDiaryForFeedPort.getDiaryById(1L)).willReturn(friendsDiary);
			given(loadDiaryForFeedPort.getPhotosForDiary(any(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(loadSocialForFeedPort.areFriends("owner-id", "friend-id")).willReturn(true);
			given(loadUserForFeedPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(loadUserForFeedPort.getUserAvatarUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForFeedPort.getUserNickname(anyString())).willReturn("owner");
			given(loadSocialForFeedPort.getLikeCount(any())).willReturn(5L);
			given(loadSocialForFeedPort.isLikedByUser(anyString(), any())).willReturn(true);
			given(loadSocialForFeedPort.countComments(any())).willReturn(3L);

			ResponseDTO result = feedQueryService.getDiaryWithPhotos(1L, requestMetaInfo, "friend-id");

			assertThat(result.getContent()).isEqualTo("친구 공개 일기");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getIsLiked()).isTrue();
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
					feedItem(30L, "writer-30", FriendStatus.FRIENDS),
					feedItem(20L, "writer-20", FriendStatus.FRIENDS)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo)))
					.willAnswer(invocation -> invocation.getArgument(0));
			given(feedCursorTokenCodec.encode(second.toCursor())).willReturn("next-token");

			FeedCursorPage<ResponseDTO> result = feedQueryService.getAllDiaries(
					new FeedCursorRequest(null, 2),
					requestMetaInfo,
					"viewer-id");

			assertThat(result.items()).extracting(ResponseDTO::getDiaryId).containsExactly(30L, 20L);
			assertThat(result.hasNext()).isTrue();
			assertThat(result.nextCursor()).isEqualTo("next-token");
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
					feedItem(30L, "friend-writer", FriendStatus.FRIENDS),
					feedItem(10L, "public-writer", FriendStatus.NONE)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo)))
					.willAnswer(invocation -> invocation.getArgument(0));
			given(feedCursorTokenCodec.encode(publicDiary.toCursor())).willReturn("next-public-token");

			FeedCursorPage<ResponseDTO> result = feedQueryService.getAllDiaries(
					new FeedCursorRequest(null, 2),
					requestMetaInfo,
					"viewer-id");

			assertThat(result.items()).extracting(ResponseDTO::getDiaryId).containsExactly(30L, 10L);
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
					.isInstanceOf(InvalidFeedCursorException.class);
		}

		private FeedCursorCandidate candidate(FeedBucket bucket, Long diaryId, long likeCount, long commentCount) {
			return new FeedCursorCandidate(bucket, diaryId, likeCount, commentCount, LocalDateTime.now().minusDays(diaryId));
		}

		private FeedListItemView feedItem(Long diaryId, String writerId, FriendStatus friendStatus) {
			return new FeedListItemView(
					diaryId,
					DiaryVisibility.PUBLIC,
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
			given(loadFeedClickPort.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(false);

			feedQueryService.logClick("user-id", 1L);

			verify(saveFeedClickPort).save(any(FeedClick.class));
		}

		@Test
		@DisplayName("이미 클릭한 경우 중복 저장하지 않는다")
		void duplicateClickIsIgnored() {
			given(loadFeedClickPort.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(true);

			feedQueryService.logClick("user-id", 1L);

			verify(saveFeedClickPort, never()).save(any());
		}
	}
}
