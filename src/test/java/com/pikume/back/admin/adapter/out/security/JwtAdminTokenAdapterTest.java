package com.pikume.back.admin.adapter.out.security;

import com.pikume.back.admin.application.port.out.AdminRefreshTokenClaims;
import com.pikume.back.security.jwt.AdminAuthConstants;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.jwt.SecurityTokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAdminTokenAdapter")
class JwtAdminTokenAdapterTest {

	@Mock
	private JwtProvider jwtProvider;

	@Test
	@DisplayName("유효한 관리자 Refresh Token의 claims를 읽는다")
	void readValidRefreshTokenReturnsClaims() {
		given(jwtProvider.validateToken("refresh-token")).willReturn(true);
		given(jwtProvider.getTokenType("refresh-token")).willReturn(SecurityTokenType.ADMIN_REFRESH);
		given(jwtProvider.getUserIdFromToken("refresh-token")).willReturn("admin-1");
		given(jwtProvider.getAdminSessionIdFromToken("refresh-token")).willReturn("session-1");

		Optional<AdminRefreshTokenClaims> result = adapter().readValidRefreshToken("refresh-token");

		assertThat(result).contains(new AdminRefreshTokenClaims("admin-1", "session-1"));
	}

	@Test
	@DisplayName("관리자 Refresh Token이 아니면 claims를 반환하지 않는다")
	void readValidRefreshTokenRejectsNonRefreshToken() {
		given(jwtProvider.validateToken("access-token")).willReturn(true);
		given(jwtProvider.getTokenType("access-token")).willReturn(SecurityTokenType.ADMIN_ACCESS);

		Optional<AdminRefreshTokenClaims> result = adapter().readValidRefreshToken("access-token");

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("관리자 토큰 TTL은 security 정책 값을 Duration으로 제공한다")
	void exposesTokenTtls() {
		JwtAdminTokenAdapter adapter = adapter();

		assertThat(adapter.accessTokenTtl()).isEqualTo(Duration.ofMillis(AdminAuthConstants.ACCESS_TOKEN_EXPIRATION_TIME));
		assertThat(adapter.refreshTokenIdleTtl()).isEqualTo(Duration.ofMillis(AdminAuthConstants.REFRESH_TOKEN_IDLE_EXPIRATION_TIME));
		assertThat(adapter.refreshTokenAbsoluteTtl()).isEqualTo(Duration.ofMillis(AdminAuthConstants.REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME));
	}

	private JwtAdminTokenAdapter adapter() {
		return new JwtAdminTokenAdapter(jwtProvider);
	}
}
