package com.pikume.back.social.adapter.out.persistence;

import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.domain.like.Like;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LikePersistenceAdapterTest {

	@InjectMocks private LikePersistenceAdapter adapter;
	@Mock private LikeJpaRepository likeJpaRepository;

	@Test
	void translatesNewLikeCollisionToSocialError() {
		Like like = Like.builder().userId("user").diaryId(1L).build();
		given(likeJpaRepository.saveAndFlush(like))
				.willThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> adapter.recordLike(like))
				.isInstanceOfSatisfying(SocialException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(SocialErrorCode.DUPLICATE_LIKE));
	}

	@Test
	void preservesTechnicalFailureWhenUpdatingExistingLike() {
		Like like = Like.builder().userId("user").diaryId(1L).build();
		ReflectionTestUtils.setField(like, "id", 10L);
		DataIntegrityViolationException failure = new DataIntegrityViolationException("storage failure");
		given(likeJpaRepository.saveAndFlush(like)).willThrow(failure);

		assertThatThrownBy(() -> adapter.recordLike(like)).isSameAs(failure);
	}
}
