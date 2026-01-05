package store.piku.back.diary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.comment.service.CommentService;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.diary.repository.FeedClickRepository;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.like.service.LikeService;
import store.piku.back.user.entity.User;

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
	private DiaryService diaryService;
	@Mock
	private CommentService commentService;
	@Mock
	private PhotoRepository photoRepository;
	@Mock
	private DiaryRepository diaryRepository;
	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;
	@Mock
	private FriendRequestService friendRequestService;
	@Mock
	private FeedClickRepository feedClickRepository;
	@Mock
	private LikeService likeService;

	private User owner;
	private Diary publicDiary;
	private Diary friendsDiary;
	private Diary privateDiary;
	private RequestMetaInfo requestMetaInfo;
	private Photo photo;

	@BeforeEach
	void setUp() {
		owner = new User("owner-id", "owner@test.com", "password", "owner", "avatar.jpg");

		publicDiary = new Diary("공개 일기 내용", Status.PUBLIC, LocalDate.now(), owner);
		friendsDiary = new Diary("친구 공개 일기", Status.FRIENDS, LocalDate.now(), owner);
		privateDiary = new Diary("비공개 일기", Status.PRIVATE, LocalDate.now(), owner);

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
			given(diaryService.getDiaryById(1L)).willReturn(publicDiary);
			given(photoRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(friendRequestService.areFriends(anyString(), anyString())).willReturn(false);
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(likeService.getLikeCount(any())).willReturn(10L);
			given(likeService.isLikedByUser(anyString(), any())).willReturn(false);
			given(commentService.countAllCommentsByDiaryId(any())).willReturn(5L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isEqualTo("공개 일기 내용");
			assertThat(result.getImgUrls()).hasSize(2);
			assertThat(result.getLikeCount()).isEqualTo(10L);
		}

		@Test
		@DisplayName("비공개 일기는 본인만 전체 내용을 조회할 수 있다")
		void privateDiaryAccessibleByOwner() {
			given(diaryService.getDiaryById(1L)).willReturn(privateDiary);
			given(photoRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg"));
			given(friendRequestService.areFriends(anyString(), anyString())).willReturn(false);
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(likeService.getLikeCount(any())).willReturn(0L);
			given(likeService.isLikedByUser(anyString(), any())).willReturn(false);
			given(commentService.countAllCommentsByDiaryId(any())).willReturn(0L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "owner-id");

			assertThat(result.getContent()).isEqualTo("비공개 일기");
		}

		@Test
		@DisplayName("비공개 일기는 타인에게 대표 사진만 보인다")
		void privateDiaryShowsOnlyThumbnailToOthers() {
			given(diaryService.getDiaryById(1L)).willReturn(privateDiary);
			given(photoRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(friendRequestService.areFriends(anyString(), anyString())).willReturn(false);
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(likeService.getLikeCount(any())).willReturn(0L);
			given(likeService.isLikedByUser(anyString(), any())).willReturn(false);
			given(commentService.countAllCommentsByDiaryId(any())).willReturn(0L);

			ResponseDTO result = feedService.getDiaryWithPhotos(1L, requestMetaInfo, "viewer-id");

			assertThat(result.getContent()).isNull();
			assertThat(result.getImgUrls()).hasSize(1);
		}

		@Test
		@DisplayName("친구 공개 일기는 친구에게 전체 내용이 보인다")
		void friendsDiaryAccessibleByFriend() {
			given(diaryService.getDiaryById(1L)).willReturn(friendsDiary);
			given(photoRepository.findByDiaryId(any())).willReturn(List.of(photo));
			given(diaryService.sortPhotos(anyList(), any())).willReturn(List.of("photo1.jpg", "photo2.jpg"));
			given(friendRequestService.areFriends("owner-id", "friend-id")).willReturn(true);
			given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("avatar-url");
			given(likeService.getLikeCount(any())).willReturn(5L);
			given(likeService.isLikedByUser(anyString(), any())).willReturn(true);
			given(commentService.countAllCommentsByDiaryId(any())).willReturn(3L);

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
