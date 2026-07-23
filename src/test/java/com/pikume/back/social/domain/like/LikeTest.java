package com.pikume.back.social.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Like")
class LikeTest {

	@Test
	@DisplayName("활성 좋아요를 취소하고 다시 활성화한다")
	void cancelsAndReactivates() {
		Like like = Like.builder().userId("user").diaryId(1L).build();

		like.cancel();
		assertThat(like.isActive()).isFalse();

		like.reactivate();
		assertThat(like.isActive()).isTrue();
	}
}
