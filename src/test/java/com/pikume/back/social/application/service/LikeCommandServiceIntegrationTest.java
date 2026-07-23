package com.pikume.back.social.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.LikeJpaRepository;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.like.Like;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

class LikeCommandServiceIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private LikeCommandService likeCommandService;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private LikeJpaRepository likeJpaRepository;

	@Autowired
	private FriendJpaRepository friendJpaRepository;

	@MockitoBean
	private PublishSocialNotificationEventPort publishEventPort;

	private String ownerId;
	private String likerId;
	private String friendId;
	private String strangerId;
	private Long diaryId;
	private Long friendsDiaryId;

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
		likeCommandService.addLike(likerId, diaryId);

		assertThatThrownBy(() -> likeCommandService.addLike(likerId, diaryId))
				.isInstanceOf(SocialException.class)
				.satisfies(e -> assertThat(((SocialException) e).getErrorCode()).isEqualTo(SocialErrorCode.ALREADY_LIKED));
	}

	@Test
	@DisplayName("좋아요 최초 저장 시 생성·수정 시각을 함께 기록한다")
	void recordsAuditTimestampsWhenLikeIsCreated() {
		likeCommandService.addLike(likerId, diaryId);
		flushAndClear();

		Like savedLike = likeJpaRepository.findByUserIdAndDiaryId(likerId, diaryId).orElseThrow();

		assertThat(savedLike.getCreatedAt()).isNotNull();
		assertThat(savedLike.getUpdatedAt()).isNotNull();
		assertThat(savedLike.getUpdatedAt()).isAfterOrEqualTo(savedLike.getCreatedAt());
	}

	@Test
	@DisplayName("좋아요하지 않은 일기에 좋아요 취소를 누르면 LIKE_NOT_FOUND 예외가 발생한다")
	void throwsWhenRemoveLikeWithoutExistingLike() {
		assertThatThrownBy(() -> likeCommandService.removeLike(likerId, diaryId))
				.isInstanceOf(SocialException.class)
				.satisfies(e -> assertThat(((SocialException) e).getErrorCode()).isEqualTo(SocialErrorCode.LIKE_NOT_FOUND));
	}

	@Test
	@DisplayName("좋아요 후 취소한 일기에 다시 좋아요를 누르면 기존 좋아요가 복구된다")
	void restoresSoftDeletedLikeWhenRelike() {
		likeCommandService.addLike(likerId, diaryId);
		then(publishEventPort).should().publish(any());
		flushAndClear();
		likeCommandService.removeLike(likerId, diaryId);
		flushAndClear();

		Like softDeletedLike = likeJpaRepository.findAll().get(0);
		Long likeId = softDeletedLike.getId();
		LocalDateTime removedUpdatedAt = softDeletedLike.getUpdatedAt();
		assertThat(softDeletedLike.getDeletedAt()).isNotNull();
		flushAndClear();

		likeCommandService.addLike(likerId, diaryId);
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
		assertThatThrownBy(() -> likeCommandService.addLike(strangerId, friendsDiaryId))
				.isInstanceOf(SocialException.class)
				.satisfies(e -> assertThat(((SocialException) e).getErrorCode()).isEqualTo(SocialErrorCode.DIARY_NOT_FOUND));
	}

	@Test
	@DisplayName("작성자는 본인 일기에 좋아요를 누를 수 있고 알림 이벤트는 발행되지 않는다")
	void ownerCanLikeOwnDiaryWithoutNotificationEvent() {
		var response = likeCommandService.addLike(ownerId, diaryId);

		assertThat(response.diaryId()).isEqualTo(diaryId);
		assertThat(response.likeCount()).isEqualTo(1L);
		assertThat(response.liked()).isTrue();
		assertThat(likeJpaRepository.findByUserIdAndDiaryId(ownerId, diaryId)).isPresent();
		then(publishEventPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("친구는 친구 공개 일기에 좋아요를 누를 수 있다")
	void friendCanLikeFriendsDiary() {
		var response = likeCommandService.addLike(friendId, friendsDiaryId);

		assertThat(response.diaryId()).isEqualTo(friendsDiaryId);
		assertThat(response.likeCount()).isEqualTo(1L);
		assertThat(response.liked()).isTrue();
		assertThat(likeJpaRepository.findByUserIdAndDiaryId(friendId, friendsDiaryId)).isPresent();
	}
}
