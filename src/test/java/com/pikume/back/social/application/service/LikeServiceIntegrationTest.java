package com.pikume.back.social.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.LikeJpaRepository;
import com.pikume.back.social.application.port.out.PublishEventPort;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.like.Like;
import com.pikume.back.social.domain.like.exception.LikeErrorCode;
import com.pikume.back.social.domain.like.exception.LikeException;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

class LikeServiceIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private LikeService likeService;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private LikeJpaRepository likeJpaRepository;

	@Autowired
	private FriendJpaRepository friendJpaRepository;

	@MockitoBean
	private PublishEventPort publishEventPort;

	private String ownerId;
	private String likerId;
	private String friendId;
	private String strangerId;
	private Long diaryId;
	private Long friendsDiaryId;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/likes/diary/1", "TestAgent", "127.0.0.1");

	@BeforeEach
	void setUp() {
		User owner = userJpaRepository.save(new User("owner@example.com", "password", "owner"));
		User liker = userJpaRepository.save(new User("liker@example.com", "password", "liker"));
		User friend = userJpaRepository.save(new User("friend@example.com", "password", "friend"));
		User stranger = userJpaRepository.save(new User("stranger@example.com", "password", "stranger"));
		Diary diary = diaryJpaRepository.save(new Diary("content", DiaryVisibility.PUBLIC, LocalDate.now(), owner.getId()));
		Diary friendsDiary = diaryJpaRepository.save(new Diary("friends", DiaryVisibility.FRIENDS, LocalDate.now(), owner.getId()));
		friendJpaRepository.save(new Friend(owner.getId(), friend.getId()));

		ownerId = owner.getId();
		likerId = liker.getId();
		friendId = friend.getId();
		strangerId = stranger.getId();
		diaryId = diary.getId();
		friendsDiaryId = friendsDiary.getId();
	}

	@Test
	@DisplayName("좋아요한 일기에 다시 좋아요를 누르면 ALREADY_LIKED 예외가 발생한다")
	void throwsWhenAddLikeToAlreadyLikedDiary() {
		likeService.addLike(likerId, diaryId, requestMetaInfo);

		assertThatThrownBy(() -> likeService.addLike(likerId, diaryId, requestMetaInfo))
				.isInstanceOf(LikeException.class)
				.satisfies(e -> assertThat(((LikeException) e).getErrorCode()).isEqualTo(LikeErrorCode.ALREADY_LIKED));
	}

	@Test
	@DisplayName("좋아요하지 않은 일기에 좋아요 취소를 누르면 LIKE_NOT_FOUND 예외가 발생한다")
	void throwsWhenRemoveLikeWithoutExistingLike() {
		assertThatThrownBy(() -> likeService.removeLike(likerId, diaryId))
				.isInstanceOf(LikeException.class)
				.satisfies(e -> assertThat(((LikeException) e).getErrorCode()).isEqualTo(LikeErrorCode.LIKE_NOT_FOUND));
	}

	@Test
	@DisplayName("좋아요 후 취소한 일기에 다시 좋아요를 누르면 기존 좋아요가 복구된다")
	void restoresSoftDeletedLikeWhenRelike() {
		likeService.addLike(likerId, diaryId, requestMetaInfo);
		then(publishEventPort).should().publish(any());
		flushAndClear();
		likeService.removeLike(likerId, diaryId);
		flushAndClear();

		Like softDeletedLike = likeJpaRepository.findAll().get(0);
		Long likeId = softDeletedLike.getId();
		LocalDateTime removedUpdatedAt = softDeletedLike.getUpdatedAt();
		assertThat(softDeletedLike.getDeletedAt()).isNotNull();
		flushAndClear();

		likeService.addLike(likerId, diaryId, requestMetaInfo);
		flushAndClear();

		Like restoredLike = likeJpaRepository.findAll().get(0);
		assertThat(restoredLike.getId()).isEqualTo(likeId);
		assertThat(restoredLike.getDeletedAt()).isNull();
		assertThat(restoredLike.getUpdatedAt()).isAfterOrEqualTo(removedUpdatedAt);
		then(publishEventPort).shouldHaveNoMoreInteractions();
	}

	@Test
	@DisplayName("비친구는 친구 공개 일기에 좋아요를 누를 수 없다")
	void strangerCannotLikeFriendsDiary() {
		assertThatThrownBy(() -> likeService.addLike(strangerId, friendsDiaryId, requestMetaInfo))
				.isInstanceOf(LikeException.class)
				.satisfies(e -> assertThat(((LikeException) e).getErrorCode()).isEqualTo(LikeErrorCode.DIARY_NOT_FOUND));
	}

	@Test
	@DisplayName("작성자는 본인 일기에 좋아요를 누를 수 있고 알림 이벤트는 발행되지 않는다")
	void ownerCanLikeOwnDiaryWithoutNotificationEvent() {
		var response = likeService.addLike(ownerId, diaryId, requestMetaInfo);

		assertThat(response.diaryId()).isEqualTo(diaryId);
		assertThat(response.likeCount()).isEqualTo(1L);
		assertThat(response.liked()).isTrue();
		assertThat(likeJpaRepository.findByUserIdAndDiaryId(ownerId, diaryId)).isPresent();
		then(publishEventPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("친구는 친구 공개 일기에 좋아요를 누를 수 있다")
	void friendCanLikeFriendsDiary() {
		var response = likeService.addLike(friendId, friendsDiaryId, requestMetaInfo);

		assertThat(response.diaryId()).isEqualTo(friendsDiaryId);
		assertThat(response.likeCount()).isEqualTo(1L);
		assertThat(response.liked()).isTrue();
		assertThat(likeJpaRepository.findByUserIdAndDiaryId(friendId, friendsDiaryId)).isPresent();
	}
}
