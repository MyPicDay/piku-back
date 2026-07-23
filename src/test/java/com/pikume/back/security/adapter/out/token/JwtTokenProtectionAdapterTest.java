package com.pikume.back.security.adapter.out.token;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("JwtTokenProtectionAdapter")
class JwtTokenProtectionAdapterTest {
	@Test
	@DisplayName("JWT 구현을 User 토큰 목적 계약으로 감싼다")
	void delegatesJwtOperations() {
		JwtTokenProvider provider = mock(JwtTokenProvider.class);
		given(provider.generateAccessToken("user-1")).willReturn("access");
		given(provider.generateRefreshToken()).willReturn("refresh");
		given(provider.validateToken("refresh")).willReturn(true);
		JwtTokenProtectionAdapter adapter = new JwtTokenProtectionAdapter(provider);

		assertThat(adapter.generateAccessToken("user-1")).isEqualTo("access");
		assertThat(adapter.generateRefreshToken()).isEqualTo("refresh");
		assertThat(adapter.isTokenValid("refresh")).isTrue();
		assertThat(adapter.accessTokenExpiresInSeconds()).isEqualTo(1800L);
		assertThat(adapter.refreshTokenExpiresInSeconds()).isEqualTo(604800L);
	}
}
