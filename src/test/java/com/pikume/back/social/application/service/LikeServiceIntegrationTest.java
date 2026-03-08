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
import com.pikume.back.social.adapter.out.persistence.LikeJpaRepository;
import com.pikume.back.social.application.port.out.PublishEventPort;
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

class LikeServiceIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private LikeService likeService;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private LikeJpaRepository likeJpaRepository;

	@MockitoBean
	private PublishEventPort publishEventPort;

	private String likerId;
	private Long diaryId;

	private final RequestMetaInfo requestMetaInfo = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080/api/likes/diary/1", "TestAgent", "127.0.0.1");

	@BeforeEach
	void setUp() {
		User owner = userJpaRepository.save(new User("owner@example.com", "password", "owner"));
		User liker = userJpaRepository.save(new User("liker@example.com", "password", "liker"));
		Diary diary = diaryJpaRepository.save(new Diary("content", DiaryVisibility.PUBLIC, LocalDate.now(), owner.getId()));

		likerId = liker.getId();
		diaryId = diary.getId();
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
	}
}
