package com.pikume.back.security.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.pikume.back.global.dto.CookieSpec;
import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.dto.ReissueResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.dto.request.LoginRequest;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.application.port.out.DeleteRefreshTokenPort;
import com.pikume.back.security.application.port.out.LoadRefreshTokenPort;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;
import com.pikume.back.security.application.port.out.RevokeDevicePushTokenPort;
import com.pikume.back.security.application.port.out.SaveRefreshTokenPort;
import com.pikume.back.security.domain.RefreshToken;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
	private RevokeDevicePushTokenPort revokeDevicePushTokenPort;
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
			AuthUserView user = new AuthUserView("user-id", "encodedPassword", "testUser", "avatar.png");
			given(loadUserForAuthPort.findByEmail("test@piku.store")).willReturn(Optional.of(user));
			given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
			given(jwtProvider.generateAccessToken("user-id")).willReturn("access-token");
			given(jwtProvider.generateRefreshToken()).willReturn("refresh-token");
			given(saveRefreshTokenPort.save(any(RefreshToken.class))).willReturn(null);

			LoginResult result = tokenService.login(request, "device-1");

			ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
			assertThat(result.tokens().getAccessToken()).isEqualTo("access-token");
			assertThat(result.tokens().getRefreshToken()).isEqualTo("refresh-token");
			assertThat(result.userInfo().id()).isEqualTo("user-id");
			assertThat(result.userInfo().nickname()).isEqualTo("testUser");
			assertThat(result.userInfo().avatarPath()).isEqualTo("avatar.png");
			then(saveRefreshTokenPort).should().save(refreshTokenCaptor.capture());
			assertThat(refreshTokenCaptor.getValue().getKey()).isEqualTo("user-id-device-1");
			assertThat(refreshTokenCaptor.getValue().getUserId()).isEqualTo("user-id");
		}

		@Test
		@DisplayName("존재하지 않는 사용자로 로그인 시 예외가 발생한다")
		void loginFailUserNotFound() {
			LoginRequest request = new LoginRequest("unknown@piku.store", "password123");
			given(loadUserForAuthPort.findByEmail("unknown@piku.store")).willReturn(Optional.empty());

			assertThatThrownBy(() -> tokenService.login(request, "device-1"))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");
		}

		@Test
		@DisplayName("비밀번호가 일치하지 않을 때 예외가 발생한다")
		void loginFailPasswordMismatch() {
			LoginRequest request = new LoginRequest("test@piku.store", "wrongPassword");
			AuthUserView user = new AuthUserView("user-id", "encodedPassword", "testUser", "avatar.png");
			given(loadUserForAuthPort.findByEmail("test@piku.store")).willReturn(Optional.of(user));
			given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

			assertThatThrownBy(() -> tokenService.login(request, "device-1"))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");
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
					.willReturn(Optional.of(new RefreshToken("user-id-device-1", "valid-refresh", "user-id")));
			given(jwtProvider.generateAccessToken("user-id")).willReturn("new-access-token");

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

		@Test
		@DisplayName("저장소에 없는 Refresh Token으로 재발급 시 null을 반환한다")
		void reissueFailWhenRefreshTokenIsNotStored() {
			given(jwtProvider.validateToken("missing-refresh")).willReturn(true);
			given(loadRefreshTokenPort.findByRefreshToken("missing-refresh")).willReturn(Optional.empty());

			String result = tokenService.reissueAccessToken("missing-refresh");

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("저장된 Refresh Token에 사용자 ID가 없으면 null을 반환하고 토큰을 삭제한다")
		void reissueFailWhenStoredRefreshTokenHasNoUserId() {
			String refreshToken = "legacy-refresh";
			given(jwtProvider.validateToken(refreshToken)).willReturn(true);
			given(loadRefreshTokenPort.findByRefreshToken(refreshToken))
					.willReturn(Optional.of(new RefreshToken("legacy-key", refreshToken, null)));

			String result = tokenService.reissueAccessToken(refreshToken);

			assertThat(result).isNull();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken(refreshToken);
			then(jwtProvider).should(never()).generateAccessToken(anyString());
		}

		@Test
		@DisplayName("유효한 Refresh Token이면 모바일용 재발급 결과를 반환한다")
		void reissueTokensReturnsMobileResult() {
			String refreshToken = "refresh-token";
			RefreshToken stored = new RefreshToken("user-1-device", refreshToken, "user-1");
			given(jwtProvider.validateToken(refreshToken)).willReturn(true);
			given(loadRefreshTokenPort.findByRefreshToken(refreshToken)).willReturn(Optional.of(stored));
			given(jwtProvider.generateAccessToken("user-1")).willReturn("new-access");

			ReissueResult result = tokenService.reissueTokens(refreshToken);

			assertThat(result.accessToken()).isEqualTo("new-access");
			assertThat(result.refreshToken()).isEqualTo(refreshToken);
			assertThat(result.accessTokenExpiresIn()).isGreaterThan(0L);
			assertThat(result.refreshTokenExpiresIn()).isGreaterThan(0L);
		}

		@Test
		@DisplayName("모바일 재발급 시 저장된 Refresh Token에 사용자 ID가 없으면 null을 반환하고 토큰을 삭제한다")
		void reissueTokensFailWhenStoredRefreshTokenHasNoUserId() {
			String refreshToken = "legacy-refresh";
			given(jwtProvider.validateToken(refreshToken)).willReturn(true);
			given(loadRefreshTokenPort.findByRefreshToken(refreshToken))
					.willReturn(Optional.of(new RefreshToken("legacy-key", refreshToken, "")));

			ReissueResult result = tokenService.reissueTokens(refreshToken);

			assertThat(result).isNull();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken(refreshToken);
			then(jwtProvider).should(never()).generateAccessToken(anyString());
		}
	}

	@Nested
	@DisplayName("logout")
	class Logout {

		@Test
		@DisplayName("로그아웃 시 Refresh Token을 삭제한다")
		void logoutSuccess() {
			tokenService.logout("user-id", "device-1");

			then(revokeDevicePushTokenPort).should().revokeDevicePushToken("user-id", "device-1");
			then(deleteRefreshTokenPort).should().deleteById("user-id-device-1");
		}

		@Test
		@DisplayName("로그아웃 시 deviceId가 비어 있으면 FCM 토큰은 삭제하지 않는다")
		void logoutSkipsPushTokenWhenDeviceIdIsBlank() {
			tokenService.logout("user-id", "");

			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
			then(deleteRefreshTokenPort).should().deleteById("user-id-");
		}

		@Test
		@DisplayName("모바일 로그아웃 시 refresh token과 같은 기기의 FCM 토큰을 삭제한다")
		void logoutByRefreshTokenSuccess() {
			given(loadRefreshTokenPort.findByRefreshToken("refresh-token"))
					.willReturn(Optional.of(new RefreshToken("user-id-device-1", "refresh-token", "user-id")));

			tokenService.logoutByRefreshToken("refresh-token", "device-1");

			then(revokeDevicePushTokenPort).should().revokeDevicePushToken("user-id", "device-1");
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("refresh-token");
		}

		@Test
		@DisplayName("모바일 로그아웃 시 빈 refresh token이면 삭제하지 않는다")
		void logoutByRefreshTokenIgnoresBlank() {
			tokenService.logoutByRefreshToken("", "device-1");

			then(deleteRefreshTokenPort).shouldHaveNoInteractions();
			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("모바일 로그아웃 시 저장된 refresh token이 없으면 FCM 토큰은 삭제하지 않는다")
		void logoutByRefreshTokenSkipsPushTokenWhenRefreshTokenIsMissing() {
			given(loadRefreshTokenPort.findByRefreshToken("refresh-token")).willReturn(Optional.empty());

			tokenService.logoutByRefreshToken("refresh-token", "device-1");

			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("refresh-token");
		}

		@Test
		@DisplayName("모바일 로그아웃 시 legacy refresh token이면 FCM 토큰은 삭제하지 않는다")
		void logoutByRefreshTokenSkipsPushTokenWhenStoredUserIdIsMissing() {
			given(loadRefreshTokenPort.findByRefreshToken("refresh-token"))
					.willReturn(Optional.of(new RefreshToken("legacy-key", "refresh-token", "")));

			tokenService.logoutByRefreshToken("refresh-token", "device-1");

			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("refresh-token");
		}

		@Test
		@DisplayName("모바일 로그아웃 시 deviceId가 없으면 FCM 토큰은 삭제하지 않는다")
		void logoutByRefreshTokenSkipsPushTokenWhenDeviceIdIsMissing() {
			given(loadRefreshTokenPort.findByRefreshToken("refresh-token"))
					.willReturn(Optional.of(new RefreshToken("user-id-device-1", "refresh-token", "user-id")));

			tokenService.logoutByRefreshToken("refresh-token", null);

			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("refresh-token");
		}

		@Test
		@DisplayName("모바일 로그아웃 시 같은 사용자의 다른 deviceId면 FCM 토큰은 삭제하지 않는다")
		void logoutByRefreshTokenSkipsPushTokenWhenDeviceDoesNotMatchRefreshToken() {
			given(loadRefreshTokenPort.findByRefreshToken("refresh-token"))
					.willReturn(Optional.of(new RefreshToken("user-id-device-1", "refresh-token", "user-id")));

			tokenService.logoutByRefreshToken("refresh-token", "device-2");

			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("refresh-token");
		}

		@Test
		@DisplayName("모바일 로그아웃 시 다른 사용자의 deviceId로 보이면 FCM 토큰은 삭제하지 않는다")
		void logoutByRefreshTokenSkipsPushTokenWhenDeviceBelongsToAnotherUser() {
			given(loadRefreshTokenPort.findByRefreshToken("refresh-token"))
					.willReturn(Optional.of(new RefreshToken("user-a-device-a", "refresh-token", "user-a")));

			tokenService.logoutByRefreshToken("refresh-token", "device-b");

			then(revokeDevicePushTokenPort).shouldHaveNoInteractions();
			then(deleteRefreshTokenPort).should().deleteByRefreshToken("refresh-token");
		}
	}

	@Nested
	@DisplayName("cookie")
	class Cookie {

		@Test
		@DisplayName("Refresh Token 쿠키를 생성한다")
		void newCookieRefreshToken() {
			CookieSpec cookie = tokenService.newCookieRefreshToken("refresh-token");

			assertThat(cookie.value()).isEqualTo("refresh-token");
			assertThat(cookie.httpOnly()).isTrue();
			assertThat(cookie.secure()).isTrue();
		}

		@Test
		@DisplayName("Refresh Token 쿠키를 제거한다")
		void removeCookieRefreshToken() {
			CookieSpec cookie = tokenService.removeCookieRefreshToken();

			assertThat(cookie.value()).isEmpty();
			assertThat(cookie.maxAgeSeconds()).isEqualTo(0);
		}
	}
}
