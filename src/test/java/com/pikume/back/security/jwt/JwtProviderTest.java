package com.pikume.back.security.jwt;

import com.pikume.back.security.config.CustomUserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtProvider")
class JwtProviderTest {

	@Mock
	private CustomUserDetailService customUserDetailService;

	private JwtProvider jwtProvider;

	@BeforeEach
	void setUp() {
		jwtProvider = new JwtProvider(customUserDetailService);
		ReflectionTestUtils.setField(jwtProvider, "secretKey", "LhVYSkvR90p9A7jPFlWWZ0uB3RPiIGnN8s3aXk2lbE4=");
	}

	@Test
	@DisplayName("사용자 Access Token에는 USER_ACCESS 타입을 포함한다")
	void userAccessTokenHasUserTokenType() {
		String token = jwtProvider.generateAccessToken("user-1");

		assertThat(jwtProvider.validateToken(token)).isTrue();
		assertThat(jwtProvider.getUserIdFromToken(token)).isEqualTo("user-1");
		assertThat(jwtProvider.getTokenType(token)).isEqualTo(SecurityTokenType.USER_ACCESS);
	}

	@Test
	@DisplayName("관리자 Access Token에는 관리자 타입, 등급, 세션 식별자를 포함한다")
	void adminAccessTokenHasAdminClaims() {
		String token = jwtProvider.generateAdminAccessToken("admin-1", "SUPER_ADMIN", "session-1");

		assertThat(jwtProvider.validateToken(token)).isTrue();
		assertThat(jwtProvider.getUserIdFromToken(token)).isEqualTo("admin-1");
		assertThat(jwtProvider.getTokenType(token)).isEqualTo(SecurityTokenType.ADMIN_ACCESS);
		assertThat(jwtProvider.getAdminRoleFromToken(token)).isEqualTo("SUPER_ADMIN");
		assertThat(jwtProvider.getAdminSessionIdFromToken(token)).isEqualTo("session-1");
	}
}
