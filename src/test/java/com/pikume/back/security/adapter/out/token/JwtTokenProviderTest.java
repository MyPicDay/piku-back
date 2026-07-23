package com.pikume.back.security.adapter.out.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider();
		ReflectionTestUtils.setField(
				jwtTokenProvider,
				"secretKey",
				"LhVYSkvR90p9A7jPFlWWZ0uB3RPiIGnN8s3aXk2lbE4=");
	}

	@Test
	@DisplayName("사용자 Access Token에서 사용자 ID를 추출한다")
	void extractsUserIdFromAccessToken() {
		String token = jwtTokenProvider.generateAccessToken("user-1");

		assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo("user-1");
	}

	@Test
	@DisplayName("사용자 ID가 없는 Refresh Token은 사용자 인증 토큰으로 사용할 수 없다")
	void rejectsRefreshTokenWithoutUserId() {
		String refreshToken = jwtTokenProvider.generateRefreshToken();

		assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
		assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(refreshToken))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("사용자 ID가 없는 토큰입니다.");
	}
}
