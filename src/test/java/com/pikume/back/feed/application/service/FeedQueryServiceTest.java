package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.diary.adapter.in.web.dto.ResponseDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.port.out.*;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
	private FeedCandidateCollector feedCandidateCollector;
	@Mock
	private FeedCompositionService feedCompositionService;

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
	@DisplayName("getAllDiaries - 피드 목록 조회")
	class GetAllDiaries {

		@Test
		@DisplayName("캐시 히트 시 전용 조회 포트로 현재 페이지 항목만 materialize 한다")
		void cachedFeedUsesDedicatedReadPort() {
			given(loadRecommendationForFeedPort.getCachedFeed("viewer-id")).willReturn(List.of(30L, 10L, 20L));
			given(loadSocialForFeedPort.getFriendIds("viewer-id")).willReturn(List.of("writer-30"));
			given(loadDiaryForFeedPort.findRestorableFeedIds(List.of(30L, 10L, 20L), "viewer-id", List.of("writer-30")))
					.willReturn(List.of(30L, 10L, 20L));
			given(loadFeedListViewPort.loadFeedListItems(List.of(30L, 10L), "viewer-id")).willReturn(List.of(
					feedItem(30L, "writer-30", FriendStatus.FRIENDS),
					feedItem(10L, "writer-10", FriendStatus.NONE)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo))).willAnswer(invocation -> invocation.getArgument(0));

			Page<ResponseDTO> result = feedQueryService.getAllDiaries(PageRequest.of(0, 2), requestMetaInfo, "viewer-id");

			assertThat(result.getTotalElements()).isEqualTo(3);
			assertThat(result.getContent()).extracting(ResponseDTO::getDiaryId).containsExactly(30L, 10L);
			verify(loadFeedListViewPort).loadFeedListItems(List.of(30L, 10L), "viewer-id");
			verify(feedCandidateCollector, never()).collect(anyString());
		}

		@Test
		@DisplayName("캐시 히트 시에도 자기 글은 제외한다")
		void cachedFeedStillExcludesOwnDiary() {
			given(loadRecommendationForFeedPort.getCachedFeed("viewer-id")).willReturn(List.of(30L, 10L, 20L));
			given(loadSocialForFeedPort.getFriendIds("viewer-id")).willReturn(List.of("writer-30"));
			given(loadDiaryForFeedPort.findRestorableFeedIds(List.of(30L, 10L, 20L), "viewer-id", List.of("writer-30")))
					.willReturn(List.of(30L, 20L));
			given(loadFeedListViewPort.loadFeedListItems(List.of(30L, 20L), "viewer-id")).willReturn(List.of(
					feedItem(30L, "writer-30", FriendStatus.FRIENDS),
					feedItem(20L, "writer-20", FriendStatus.NONE)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo))).willAnswer(invocation -> invocation.getArgument(0));

			Page<ResponseDTO> result = feedQueryService.getAllDiaries(PageRequest.of(0, 10), requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).extracting(ResponseDTO::getDiaryId).containsExactly(30L, 20L);
		}

		@Test
		@DisplayName("캐시 히트 시 PRIVATE 일기는 복원 대상에서 제외한다")
		void cachedFeedExcludesPrivateDiaries() {
			given(loadRecommendationForFeedPort.getCachedFeed("viewer-id")).willReturn(List.of(30L, 10L, 20L));
			given(loadSocialForFeedPort.getFriendIds("viewer-id")).willReturn(List.of("writer-30"));
			given(loadDiaryForFeedPort.findRestorableFeedIds(List.of(30L, 10L, 20L), "viewer-id", List.of("writer-30")))
					.willReturn(List.of(30L, 20L));
			given(loadFeedListViewPort.loadFeedListItems(List.of(30L, 20L), "viewer-id")).willReturn(List.of(
					feedItem(30L, "writer-30", FriendStatus.FRIENDS),
					feedItem(20L, "writer-20", FriendStatus.NONE)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo))).willAnswer(invocation -> invocation.getArgument(0));

			Page<ResponseDTO> result = feedQueryService.getAllDiaries(PageRequest.of(0, 10), requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).extracting(ResponseDTO::getDiaryId).containsExactly(30L, 20L);
		}

		@Test
		@DisplayName("캐시 미스 시 후보를 점수화하고 캐시한 뒤 전용 조회 포트로 변환한다")
		void cacheMissCollectsScoresAndCachesIds() {
			FeedCandidateCollector.FeedCandidates candidates = new FeedCandidateCollector.FeedCandidates(
					List.of(10L, 20L, 30L),
					List.of(10L),
					List.of(20L, 30L));

			given(loadRecommendationForFeedPort.getCachedFeed("viewer-id")).willReturn(List.of());
			given(feedCandidateCollector.collect("viewer-id")).willReturn(candidates);
			given(feedCompositionService.composeFeed("viewer-id", List.of(10L), List.of(20L, 30L), 3))
					.willReturn(List.of(20L, 10L, 30L));
			given(loadFeedListViewPort.loadFeedListItems(List.of(20L, 10L), "viewer-id")).willReturn(List.of(
					feedItem(20L, "writer-20", FriendStatus.REQUESTED),
					feedItem(10L, "writer-10", FriendStatus.FRIENDS)));
			given(loadUserForFeedPort.getUserAvatarUrl(anyString(), eq(requestMetaInfo))).willAnswer(invocation -> invocation.getArgument(0));

			Page<ResponseDTO> result = feedQueryService.getAllDiaries(PageRequest.of(0, 2), requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).extracting(ResponseDTO::getDiaryId).containsExactly(20L, 10L);
			verify(loadRecommendationForFeedPort).cacheFeed("viewer-id", List.of(20L, 10L, 30L));
			verify(loadFeedListViewPort).loadFeedListItems(List.of(20L, 10L), "viewer-id");
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
