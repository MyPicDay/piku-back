package com.pikume.back.social.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.adapter.in.web.dto.LikeResponse;
import com.pikume.back.social.application.port.out.LoadDiaryInfoPort;
import com.pikume.back.social.application.port.out.LoadLikePort;
import com.pikume.back.social.application.port.out.PublishEventPort;
import com.pikume.back.social.application.port.out.SaveLikePort;
import com.pikume.back.social.domain.event.SocialEvent;
import com.pikume.back.social.domain.like.Like;
import com.pikume.back.social.domain.like.exception.LikeErrorCode;
import com.pikume.back.social.domain.like.exception.LikeException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

	@InjectMocks
	private LikeService likeService;

	@Mock
	private LoadLikePort loadLikePort;

	@Mock
	private SaveLikePort saveLikePort;

	@Mock
	private LoadDiaryInfoPort loadDiaryInfoPort;

	@Mock
	private PublishEventPort publishEventPort;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/likes/diary/1", "TestAgent", "127.0.0.1");

	@Nested
	@DisplayName("addLike - 좋아요 추가")
	class AddLike {

		@Test
		@DisplayName("성공적으로 좋아요를 추가하고 이벤트를 발행한다")
		void addLikeSuccess() {
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadLikePort.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.empty());
			given(loadLikePort.countByDiaryId(1L)).willReturn(1L);

			LikeResponse response = likeService.addLike("liker-id", 1L, requestMetaInfo);

			assertThat(response.getDiaryId()).isEqualTo(1L);
			assertThat(response.getLikeCount()).isEqualTo(1L);
			assertThat(response.isLiked()).isTrue();
			then(saveLikePort).should().save(any(Like.class));
			then(publishEventPort).should().publish(any(SocialEvent.LikeCreatedEvent.class));
		}

		@Test
		@DisplayName("존재하지 않는 일기에 좋아요하면 예외 발생")
		void failsDiaryNotFound() {
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> likeService.addLike("liker-id", 999L, requestMetaInfo))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> assertThat(((LikeException) e).getErrorCode())
							.isEqualTo(LikeErrorCode.DIARY_NOT_FOUND));
		}

		@Test
		@DisplayName("본인 일기에 좋아요하면 예외 발생")
		void failsOwnDiary() {
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));

			assertThatThrownBy(() -> likeService.addLike("owner-id", 1L, requestMetaInfo))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> assertThat(((LikeException) e).getErrorCode())
							.isEqualTo(LikeErrorCode.CANNOT_LIKE_OWN_DIARY));
		}

		@Test
		@DisplayName("이미 좋아요한 일기에 다시 좋아요하면 예외 발생")
		void failsAlreadyLiked() {
			Like existingLike = Like.builder().userId("liker-id").diaryId(1L).build();
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadLikePort.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.of(existingLike));

			assertThatThrownBy(() -> likeService.addLike("liker-id", 1L, requestMetaInfo))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> assertThat(((LikeException) e).getErrorCode())
							.isEqualTo(LikeErrorCode.ALREADY_LIKED));
		}

		@Test
		@DisplayName("좋아요 추가 시 이벤트가 발행되지 않으면 안 된다")
		void publishesEvent() {
			given(loadDiaryInfoPort.findOwnerUserIdByDiaryId(1L)).willReturn(Optional.of("owner-id"));
			given(loadLikePort.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.empty());
			given(loadLikePort.countByDiaryId(1L)).willReturn(1L);

			likeService.addLike("liker-id", 1L, requestMetaInfo);

			then(publishEventPort).should().publish(any(SocialEvent.LikeCreatedEvent.class));
		}
	}

	@Nested
	@DisplayName("removeLike - 좋아요 취소")
	class RemoveLike {

		@Test
		@DisplayName("성공적으로 좋아요를 취소한다")
		void removeLikeSuccess() {
			Like existingLike = Like.builder().userId("liker-id").diaryId(1L).build();
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);
			given(loadLikePort.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.of(existingLike));
			given(loadLikePort.countByDiaryId(1L)).willReturn(0L);

			LikeResponse response = likeService.removeLike("liker-id", 1L);

			assertThat(response.getDiaryId()).isEqualTo(1L);
			assertThat(response.getLikeCount()).isEqualTo(0L);
			assertThat(response.isLiked()).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 일기의 좋아요 취소 시 예외 발생")
		void failsDiaryNotFound() {
			given(loadDiaryInfoPort.existsById(999L)).willReturn(false);

			assertThatThrownBy(() -> likeService.removeLike("liker-id", 999L))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> assertThat(((LikeException) e).getErrorCode())
							.isEqualTo(LikeErrorCode.DIARY_NOT_FOUND));
		}

		@Test
		@DisplayName("좋아요하지 않은 일기 취소 시 예외 발생")
		void failsNotLiked() {
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);
			given(loadLikePort.findByUserIdAndDiaryId("liker-id", 1L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> likeService.removeLike("liker-id", 1L))
					.isInstanceOf(LikeException.class)
					.satisfies(e -> assertThat(((LikeException) e).getErrorCode())
							.isEqualTo(LikeErrorCode.LIKE_NOT_FOUND));
		}
	}

	@Nested
	@DisplayName("getLikeStatus - 좋아요 상태 조회")
	class GetLikeStatus {

		@Test
		@DisplayName("로그인 사용자가 좋아요한 일기의 상태를 조회한다")
		void loggedInUserWhoLiked() {
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);
			given(loadLikePort.countByDiaryId(1L)).willReturn(5L);
			given(loadLikePort.existsByUserIdAndDiaryId("liker-id", 1L)).willReturn(true);

			LikeResponse response = likeService.getLikeStatus("liker-id", 1L);

			assertThat(response.getDiaryId()).isEqualTo(1L);
			assertThat(response.getLikeCount()).isEqualTo(5L);
			assertThat(response.isLiked()).isTrue();
		}

		@Test
		@DisplayName("비로그인 사용자가 일기의 좋아요 상태를 조회한다")
		void anonymousUser() {
			given(loadDiaryInfoPort.existsById(1L)).willReturn(true);
			given(loadLikePort.countByDiaryId(1L)).willReturn(5L);

			LikeResponse response = likeService.getLikeStatus(null, 1L);

			assertThat(response.getLikeCount()).isEqualTo(5L);
			assertThat(response.isLiked()).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 일기 조회 시 예외 발생")
		void failsDiaryNotFound() {
			given(loadDiaryInfoPort.existsById(999L)).willReturn(false);

			assertThatThrownBy(() -> likeService.getLikeStatus("user-id", 999L))
					.isInstanceOf(LikeException.class);
		}
	}

	@Nested
	@DisplayName("getLikeCount / isLikedByUser - 단일 조회")
	class SimpleQueries {

		@Test
		@DisplayName("좋아요 수를 조회한다")
		void getLikeCount() {
			given(loadLikePort.countByDiaryId(1L)).willReturn(10L);

			assertThat(likeService.getLikeCount(1L)).isEqualTo(10L);
		}

		@Test
		@DisplayName("사용자가 좋아요했는지 확인한다")
		void isLikedByUser() {
			given(loadLikePort.existsByUserIdAndDiaryId("user-id", 1L)).willReturn(true);

			assertThat(likeService.isLikedByUser("user-id", 1L)).isTrue();
		}

		@Test
		@DisplayName("비로그인 사용자의 좋아요 여부는 false를 반환한다")
		void isLikedByNullUser() {
			assertThat(likeService.isLikedByUser(null, 1L)).isFalse();
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
			given(loadLikePort.countByDiaryIds(diaryIds)).willReturn(mockResults);

			Map<Long, Long> result = likeService.getLikeCountsForDiaries(diaryIds);

			assertThat(result).hasSize(3);
			assertThat(result.get(1L)).isEqualTo(5L);
			assertThat(result.get(2L)).isEqualTo(3L);
			assertThat(result.get(3L)).isEqualTo(0L);
		}

		@Test
		@DisplayName("빈 리스트로 조회하면 빈 Map을 반환한다")
		void getLikeCountsForEmptyList() {
			assertThat(likeService.getLikeCountsForDiaries(List.of())).isEmpty();
		}

		@Test
		@DisplayName("null 리스트로 조회하면 빈 Map을 반환한다")
		void getLikeCountsForNullList() {
			assertThat(likeService.getLikeCountsForDiaries(null)).isEmpty();
		}

		@Test
		@DisplayName("사용자가 좋아요한 일기 ID 목록을 조회한다")
		void getLikedDiaryIds() {
			List<Long> diaryIds = List.of(1L, 2L, 3L);
			given(loadLikePort.findLikedDiaryIdsByUserIdAndDiaryIds("user-id", diaryIds))
					.willReturn(Set.of(1L, 3L));

			Set<Long> result = likeService.getLikedDiaryIds("user-id", diaryIds);

			assertThat(result).containsExactlyInAnyOrder(1L, 3L);
		}

		@Test
		@DisplayName("비로그인 사용자의 좋아요 일기 ID 조회는 빈 Set을 반환한다")
		void getLikedDiaryIdsForAnonymous() {
			assertThat(likeService.getLikedDiaryIds(null, List.of(1L, 2L, 3L))).isEmpty();
		}
	}
}
