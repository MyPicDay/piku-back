package store.piku.back.like.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.like.dto.LikeResponse;
import store.piku.back.like.entity.Like;
import store.piku.back.like.exception.LikeErrorCode;
import store.piku.back.like.exception.LikeException;
import store.piku.back.like.repository.LikeRepository;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.service.NotificationService;
import store.piku.back.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

	@InjectMocks
	private LikeService likeService;

	@Mock
	private LikeRepository likeRepository;

	@Mock
	private DiaryRepository diaryRepository;

	@Mock
	private NotificationService notificationService;

	private User diaryOwner;
	private Diary diary;
	private RequestMetaInfo requestMetaInfo;

	@BeforeEach
	void setUp() {
		diaryOwner = new User("owner-id", "owner@test.com", "password", "owner", null);
		diary = new Diary("테스트 일기 내용", Status.PUBLIC, LocalDate.now(), diaryOwner);
		requestMetaInfo = new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
				"https://localhost:8080/api/likes/diary/1", "TestAgent", "127.0.0.1");
	}

	@Nested
	@DisplayName("좋아요 추가")
	class AddLike {

		@Test
		@DisplayName("성공적으로 좋아요를 추가하고 알림을 전송한다")
		void addLikeSuccess() {
			given(diaryRepository.findById(1L)).willReturn(Optional.of(diary));
			given(likeRepository.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.empty());
			given(likeRepository.save(any(Like.class))).willAnswer(invocation -> invocation.getArgument(0));
			given(likeRepository.countByDiaryId(1L)).willReturn(1L);

			LikeResponse response = likeService.addLike("liker-id", 1L, requestMetaInfo);

			assertThat(response.getDiaryId()).isEqualTo(1L);
			assertThat(response.getLikeCount()).isEqualTo(1L);
			assertThat(response.isLiked()).isTrue();
			verify(notificationService).sendNotification(
					eq("owner-id"),
					eq(NotificationType.LIKE),
					eq("liker-id"),
					eq(diary),
					eq(requestMetaInfo));
		}

		@Test
		@DisplayName("존재하지 않는 일기에 좋아요하면 예외 발생")
		void addLikeFailsDiaryNotFound() {
			given(diaryRepository.findById(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> likeService.addLike("liker-id", 999L, requestMetaInfo))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> {
						LikeException ex = (LikeException) e;
						assertThat(ex.getErrorCode()).isEqualTo(LikeErrorCode.DIARY_NOT_FOUND);
					});
		}

		@Test
		@DisplayName("본인 일기에 좋아요하면 예외 발생")
		void addLikeFailsOwnDiary() {
			given(diaryRepository.findById(1L)).willReturn(Optional.of(diary));

			assertThatThrownBy(() -> likeService.addLike("owner-id", 1L, requestMetaInfo))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> {
						LikeException ex = (LikeException) e;
						assertThat(ex.getErrorCode()).isEqualTo(LikeErrorCode.CANNOT_LIKE_OWN_DIARY);
					});
		}

		@Test
		@DisplayName("이미 좋아요한 일기에 다시 좋아요하면 예외 발생")
		void addLikeFailsAlreadyLiked() {
			Like existingLike = Like.builder().userId("liker-id").diaryId(1L).build();
			given(diaryRepository.findById(1L)).willReturn(Optional.of(diary));
			given(likeRepository.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.of(existingLike));

			assertThatThrownBy(() -> likeService.addLike("liker-id", 1L, requestMetaInfo))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> {
						LikeException ex = (LikeException) e;
						assertThat(ex.getErrorCode()).isEqualTo(LikeErrorCode.ALREADY_LIKED);
					});
		}
	}

	@Nested
	@DisplayName("좋아요 취소")
	class RemoveLike {

		@Test
		@DisplayName("성공적으로 좋아요를 취소한다")
		void removeLikeSuccess() {
			Like existingLike = Like.builder().userId("liker-id").diaryId(1L).build();
			given(diaryRepository.existsById(1L)).willReturn(true);
			given(likeRepository.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.of(existingLike));
			given(likeRepository.countByDiaryId(1L)).willReturn(0L);

			LikeResponse response = likeService.removeLike("liker-id", 1L);

			assertThat(response.getDiaryId()).isEqualTo(1L);
			assertThat(response.getLikeCount()).isEqualTo(0L);
			assertThat(response.isLiked()).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 일기의 좋아요 취소 시 예외 발생")
		void removeLikeFailsDiaryNotFound() {
			given(diaryRepository.existsById(999L)).willReturn(false);

			assertThatThrownBy(() -> likeService.removeLike("liker-id", 999L))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> {
						LikeException ex = (LikeException) e;
						assertThat(ex.getErrorCode()).isEqualTo(LikeErrorCode.DIARY_NOT_FOUND);
					});
		}

		@Test
		@DisplayName("좋아요하지 않은 일기의 좋아요 취소 시 예외 발생")
		void removeLikeFailsNotLiked() {
			given(diaryRepository.existsById(1L)).willReturn(true);
			given(likeRepository.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> likeService.removeLike("liker-id", 1L))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> {
						LikeException ex = (LikeException) e;
						assertThat(ex.getErrorCode()).isEqualTo(LikeErrorCode.LIKE_NOT_FOUND);
					});
		}
	}

	@Nested
	@DisplayName("좋아요 조회")
	class GetLikeStatus {

		@Test
		@DisplayName("로그인 사용자가 좋아요한 일기의 상태를 조회한다")
		void getLikeStatusForLoggedInUserWhoLiked() {
			given(diaryRepository.existsById(1L)).willReturn(true);
			given(likeRepository.countByDiaryId(1L)).willReturn(5L);
			given(likeRepository.existsByUserIdAndDiaryId("liker-id", 1L)).willReturn(true);

			LikeResponse response = likeService.getLikeStatus("liker-id", 1L);

			assertThat(response.getDiaryId()).isEqualTo(1L);
			assertThat(response.getLikeCount()).isEqualTo(5L);
			assertThat(response.isLiked()).isTrue();
		}

		@Test
		@DisplayName("로그인 사용자가 좋아요하지 않은 일기의 상태를 조회한다")
		void getLikeStatusForLoggedInUserWhoNotLiked() {
			given(diaryRepository.existsById(1L)).willReturn(true);
			given(likeRepository.countByDiaryId(1L)).willReturn(5L);
			given(likeRepository.existsByUserIdAndDiaryId("liker-id", 1L)).willReturn(false);

			LikeResponse response = likeService.getLikeStatus("liker-id", 1L);

			assertThat(response.isLiked()).isFalse();
		}

		@Test
		@DisplayName("비로그인 사용자가 일기의 좋아요 상태를 조회한다")
		void getLikeStatusForAnonymousUser() {
			given(diaryRepository.existsById(1L)).willReturn(true);
			given(likeRepository.countByDiaryId(1L)).willReturn(5L);

			LikeResponse response = likeService.getLikeStatus(null, 1L);

			assertThat(response.getLikeCount()).isEqualTo(5L);
			assertThat(response.isLiked()).isFalse();
		}
	}

	@Nested
	@DisplayName("배치 조회")
	class BatchQueries {

		@Test
		@DisplayName("여러 일기의 좋아요 수를 한 번에 조회한다")
		void getLikeCountsForDiaries() {
			List<Long> diaryIds = List.of(1L, 2L, 3L);
			List<Object[]> mockResults = List.of(
					new Object[] { 1L, 5L },
					new Object[] { 2L, 3L },
					new Object[] { 3L, 0L });
			given(likeRepository.countByDiaryIds(diaryIds)).willReturn(mockResults);

			Map<Long, Long> result = likeService.getLikeCountsForDiaries(diaryIds);

			assertThat(result).hasSize(3);
			assertThat(result.get(1L)).isEqualTo(5L);
			assertThat(result.get(2L)).isEqualTo(3L);
			assertThat(result.get(3L)).isEqualTo(0L);
		}

		@Test
		@DisplayName("빈 리스트로 조회하면 빈 Map을 반환한다")
		void getLikeCountsForEmptyList() {
			Map<Long, Long> result = likeService.getLikeCountsForDiaries(List.of());

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("사용자가 좋아요한 일기 ID 목록을 조회한다")
		void getLikedDiaryIds() {
			List<Long> diaryIds = List.of(1L, 2L, 3L);
			given(likeRepository.findLikedDiaryIdsByUserIdAndDiaryIds("liker-id", diaryIds))
					.willReturn(Set.of(1L, 3L));

			Set<Long> result = likeService.getLikedDiaryIds("liker-id", diaryIds);

			assertThat(result).containsExactlyInAnyOrder(1L, 3L);
		}

		@Test
		@DisplayName("비로그인 사용자의 좋아요 일기 ID 조회는 빈 Set을 반환한다")
		void getLikedDiaryIdsForAnonymous() {
			Set<Long> result = likeService.getLikedDiaryIds(null, List.of(1L, 2L, 3L));

			assertThat(result).isEmpty();
		}
	}
}
