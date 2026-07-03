package com.pikume.back.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User")
class UserTest {

	@Test
	@DisplayName("회원 탈퇴 시 탈퇴 처리 시각을 기록한다")
	void recordsWithdrawnAtWhenUserWithdraws() {
		User user = new User("user@example.com", "password", "nickname");

		user.withdraw();

		assertThat(user.getDeletedAt()).isNotNull();
		assertThat(user.isWithdrawn()).isTrue();
	}
}
