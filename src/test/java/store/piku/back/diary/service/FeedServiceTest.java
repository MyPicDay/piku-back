package store.piku.back.diary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.social.application.port.in.CommentUseCase;
import store.piku.back.diary.adapter.in.web.dto.ResponseDTO;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.Photo;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.diary.application.port.out.LoadUserForDiaryPort;
import store.piku.back.diary.application.service.DiaryQueryService;
import store.piku.back.diary.adapter.out.persistence.DiaryJpaRepository;
import store.piku.back.diary.repository.FeedClickRepository;
import store.piku.back.diary.adapter.out.persistence.PhotoJpaRepository;
import store.piku.back.social.application.port.in.FriendUseCase;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.social.application.port.in.LikeUseCase;
import store.piku.back.recommendation._legacy.FeedCandidateCollector;
import store.piku.back.recommendation._legacy.FeedCompositionService;
import store.piku.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import store.piku.back.recommendation.application.port.in.CacheFeedUseCase;
import store.piku.back.recommendation.application.port.in.ManageUserPreferenceUseCase;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

	@InjectMocks
	private FeedService feedService;

	@Mock
	private DiaryQueryService diaryQueryService;
	@Mock
	private CommentUseCase commentUseCase;
	@Mock
	private PhotoJpaRepository photoJpaRepository;
	@Mock
	private DiaryJpaRepository diaryJpaRepository;
	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;
	@Mock
	private FriendUseCase friendUseCase;
	@Mock
	private FeedClickRepository feedClickRepository;
	@Mock
	private LikeUseCase likeUseCase;
	@Mock
	private FeedCandidateCollector feedCandidateCollector;
	@Mock
	private FeedCompositionService feedCompositionService;
	@Mock
	private CacheFeedUseCase cacheFeedUseCase;
	@Mock
	private ManageUserPreferenceUseCase manageUserPreferenceUseCase;
	@Mock
	private AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;
	@Mock
	private LoadUserForDiaryPort loadUserForDiaryPort;

	private Diary publicDiary;
	private Diary friendsDiary;
	private Diary privateDiary;
	private RequestMetaInfo requestMetaInfo;
	private Photo photo;

	@BeforeEach
	void setUp() {
		publicDiary = new Diary("공개 일기 내용", DiaryVisibility.PUBLIC, LocalDate.now(), "owner-id");
		friendsDiary = new Diary("친구 공개 일기", DiaryVisibility.FRIENDS, LocalDate.now(), "owner-id");
		privateDiary = new Diary("비공개 일기", DiaryVisibility.PRIVATE, LocalDate.now(), "owner-id");

		photo = new Photo();
		requestMetaInfo = new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
				"https://localhost:8080/api/diary", "TestAgent", "127.0.0.1");
	}

	@Nested
	@DisplayName("getDiaryWithPhotos - 단일 일기 상세 조회")
	class GetDiaryWithPhotos {

		@Test
		@DisplayName("공개 일기는 누구나 전체 내용을 조회할 수 있다")
		void publicDiaryAccessibleByAnyone() {
			given(diaryQueryService.getDiaryById(1L)).willReturn(publicDiary);
			given(photoJpaRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryQueryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(friendUseCase.areFriends(anyString(), anyString())).willReturn(false);
			given(loadUserForDiaryPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForDiaryPort.getUserNickname(anyString())).willReturn("owner");
			given(likeUseCase.getLikeCount(any())).willReturn(10L);
			given(likeUseCase.isLikedByUser(anyString(), any())).willReturn(false);
			given(commentUseCase.countAllCommentsByDiaryId(any())).willReturn(5L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isEqualTo("공개 일기 내용");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getLikeCount()).isEqualTo(10L);
		}

		@Test
		@DisplayName("비공개 일기는 본인만 전체 내용을 조회할 수 있다")
		void privateDiaryAccessibleByOwner() {
			given(diaryQueryService.getDiaryById(1L)).willReturn(privateDiary);
			given(photoJpaRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryQueryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg"));
			given(friendUseCase.areFriends(anyString(), anyString())).willReturn(false);
			given(loadUserForDiaryPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForDiaryPort.getUserNickname(anyString())).willReturn("owner");
			given(likeUseCase.getLikeCount(any())).willReturn(0L);
			given(likeUseCase.isLikedByUser(anyString(), any())).willReturn(false);
			given(commentUseCase.countAllCommentsByDiaryId(any())).willReturn(0L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "owner-id");

			assertThat(result.getContent()).isEqualTo("비공개 일기");
		}

		@Test
		@DisplayName("비공개 일기는 타인에게 대표 사진만 보인다")
		void privateDiaryShowsOnlyThumbnailToOthers() {
			given(diaryQueryService.getDiaryById(1L)).willReturn(privateDiary);
			given(photoJpaRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryQueryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(friendUseCase.areFriends(anyString(), anyString())).willReturn(false);
			given(loadUserForDiaryPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForDiaryPort.getUserNickname(anyString())).willReturn("owner");
			given(likeUseCase.getLikeCount(any())).willReturn(0L);
			given(likeUseCase.isLikedByUser(anyString(), any())).willReturn(false);
			given(commentUseCase.countAllCommentsByDiaryId(any())).willReturn(0L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isNull();
			assertThat(result.getImgUrls()).hasSize(1);
		}

		@Test
		@DisplayName("친구 공개 일기는 친구에게 전체 내용이 보인다")
		void friendsDiaryAccessibleByFriend() {
			given(diaryQueryService.getDiaryById(1L)).willReturn(friendsDiary);
			given(photoJpaRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryQueryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(friendUseCase.areFriends("owner-id", "friend-id")).willReturn(true);
			given(loadUserForDiaryPort.getUserAvatar(anyString())).willReturn("avatar.jpg");
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(loadUserForDiaryPort.getUserNickname(anyString())).willReturn("owner");
			given(likeUseCase.getLikeCount(any())).willReturn(5L);
			given(likeUseCase.isLikedByUser(anyString(), any())).willReturn(true);
			given(commentUseCase.countAllCommentsByDiaryId(any())).willReturn(3L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "friend-id");

			assertThat(result.getContent()).isEqualTo("친구 공개 일기");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getIsLiked()).isTrue();
		}
	}

	@Nested
	@DisplayName("logClick - 클릭 로그 기록")
	class LogClick {

		@Test
		@DisplayName("처음 클릭하면 로그가 저장된다")
		void firstClickSavesLog() {
			given(feedClickRepository.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(false);

			feedService.logClick("user-id", 1L);

			verify(feedClickRepository).save(any());
		}

		@Test
		@DisplayName("이미 클릭한 경우 중복 저장하지 않는다")
		void duplicateClickIsIgnored() {
			given(feedClickRepository.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(true);

			feedService.logClick("user-id", 1L);

			verify(feedClickRepository, never()).save(any());
		}
	}
}
