package com.pikume.back.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails")
class CustomUserDetailsTest {

	@Test
	@DisplayName("Spring Security username은 인증 주체인 사용자 ID를 반환한다")
	void usernameReturnsUserId() {
		CustomUserDetails userDetails = new CustomUserDetails(
				"user-id",
				"pikume");

		assertThat(userDetails.getUsername()).isEqualTo("user-id");
	}
}
