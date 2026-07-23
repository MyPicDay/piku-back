package com.pikume.back.security.principal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserPrincipal")
class UserPrincipalTest {

	@Test
	@DisplayName("Spring Security username은 인증 주체인 사용자 ID를 반환한다")
	void usernameReturnsUserId() {
		UserPrincipal principal = new UserPrincipal(
				"user-id",
				"pikume");

		assertThat(principal.getUsername()).isEqualTo("user-id");
	}
}
