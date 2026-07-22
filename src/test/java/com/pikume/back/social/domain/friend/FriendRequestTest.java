package com.pikume.back.social.domain.friend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FriendRequest")
class FriendRequestTest {

	@Test
	@DisplayName("서로 다른 사용자 사이의 대기 요청을 만든다")
	void createsPendingRequest() {
		FriendRequest request = new FriendRequest("sender", "receiver");

		assertThat(request.getFromUserId()).isEqualTo("sender");
		assertThat(request.getToUserId()).isEqualTo("receiver");
	}

	@Test
	@DisplayName("자기 자신에게 친구 요청을 만들 수 없다")
	void rejectsSelfRequest() {
		assertThatThrownBy(() -> new FriendRequest("same-user", "same-user"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
