package store.piku.back.security.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import store.piku.back.security.dto.TokenDto;
import store.piku.back.security.dto.request.LoginRequest;
import store.piku.back.security.jwt.JwtProvider;
import store.piku.back.security.application.port.out.DeleteRefreshTokenPort;
import store.piku.back.security.application.port.out.LoadRefreshTokenPort;
import store.piku.back.security.application.port.out.LoadUserForAuthPort;
import store.piku.back.security.application.port.out.SaveRefreshTokenPort;
import store.piku.back.security.domain.RefreshToken;
import store.piku.back.user.domain.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService")
class TokenServiceTest {

	@InjectMocks
	private TokenService tokenService;

	@Mock
	private LoadUserForAuthPort loadUserForAuthPort;
	@Mock
	private LoadRefreshTokenPort loadRefreshTokenPort;
	@Mock
	private SaveRefreshTokenPort saveRefreshTokenPort;
	@Mock
	private DeleteRefreshTokenPort deleteRefreshTokenPort;
	@Mock
	private JwtProvider jwtProvider;
	@Mock
	private PasswordEncoder passwordEncoder;

	@Nested
	@DisplayName("login")
	class Login {

		@Test
		@DisplayName("유효한 이메일과 비밀번호로 로그인 성공 시 토큰을 반환한다")
		void loginSuccess() {
			LoginRequest request = new LoginRequest("test@piku.store", "password123");
			User user = new User("test@piku.store", "encodedPassword", "testUser");
			given(loadUserForAuthPort.findByEmail("test@piku.store")).willReturn(Optional.of(user));
			given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
			given(jwtProvider.generateAccessToken("test@piku.store")).willReturn("access-token");
			given(jwtProvider.generateRefreshToken()).willReturn("refresh-token");
			given(saveRefreshTokenPort.save(any(RefreshToken.class))).willReturn(null);

			TokenDto result = tokenService.login(request, "device-1");

			assertThat(result.getAccessToken()).isEqualTo("access-token");
			assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
			then(saveRefreshTokenPort).should().save(any(RefreshToken.class));
		}

		@Test
		@DisplayName("존재하지 않는 사용자로 로그인 시 예외가 발생한다")
		void loginFailUserNotFound() {
			LoginRequest request = new LoginRequest("unknown@piku.store", "password123");
			given(loadUserForAuthPort.findByEmail("unknown@piku.store")).willReturn(Optional.empty());

			assertThatThrownBy(() -> tokenService.login(request, "device-1"))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("사용자를 찾을 수 없습니다");
		}

		@Test
		@DisplayName("비밀번호가 일치하지 않을 때 예외가 발생한다")
		void loginFailPasswordMismatch() {
			LoginRequest request = new LoginRequest("test@piku.store", "wrongPassword");
			User user = new User("test@piku.store", "encodedPassword", "testUser");
			given(loadUserForAuthPort.findByEmail("test@piku.store")).willReturn(Optional.of(user));
			given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

			assertThatThrownBy(() -> tokenService.login(request, "device-1"))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("비밀번호가 일치하지 않습니다");
		}
	}

	@Nested
	@DisplayName("reissueAccessToken")
	class ReissueAccessToken {

		@Test
		@DisplayName("유효한 Refresh Token으로 Access Token을 재발급한다")
		void reissueSuccess() {
			given(jwtProvider.validateToken("valid-refresh")).willReturn(true);
			given(loadRefreshTokenPort.findByRefreshToken("valid-refresh"))
					.willReturn(Optional.of(new RefreshToken("test@piku.store-device-1", "valid-refresh", "user-id")));
			given(jwtProvider.generateAccessToken("test@piku.store")).willReturn("new-access-token");

			String result = tokenService.reissueAccessToken("valid-refresh");

			assertThat(result).isEqualTo("new-access-token");
		}

		@Test
		@DisplayName("만료된 Refresh Token으로 재발급 시 null을 반환하고 토큰을 삭제한다")
		void reissueFailExpiredToken() {
			given(jwtProvider.validateToken("expired-refresh")).willReturn(false);

			String result = tokenService.reissueAccessToken("expired-refresh");

			assertThat(result).isNull();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("expired-refresh");
		}

		@Test
		@DisplayName("빈 Refresh Token으로 재발급 시 null을 반환한다")
		void reissueFailEmptyToken() {
			String result = tokenService.reissueAccessToken("");

			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("logout")
	class Logout {

		@Test
		@DisplayName("로그아웃 시 Refresh Token을 삭제한다")
		void logoutSuccess() {
			tokenService.logout("test@piku.store", "device-1");

			then(deleteRefreshTokenPort).should().deleteById("test@piku.store-device-1");
		}
	}

	@Nested
	@DisplayName("cookie")
	class Cookie {

		@Test
		@DisplayName("Refresh Token 쿠키를 생성한다")
		void newCookieRefreshToken() {
			ResponseCookie cookie = tokenService.newCookieRefreshToken("refresh-token");

			assertThat(cookie.getValue()).isEqualTo("refresh-token");
			assertThat(cookie.isHttpOnly()).isTrue();
			assertThat(cookie.isSecure()).isTrue();
		}

		@Test
		@DisplayName("Refresh Token 쿠키를 제거한다")
		void removeCookieRefreshToken() {
			ResponseCookie cookie = tokenService.removeCookieRefreshToken();

			assertThat(cookie.getValue()).isEmpty();
			assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0);
		}
	}
}
