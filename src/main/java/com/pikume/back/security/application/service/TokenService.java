package com.pikume.back.security.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.pikume.back.user.auth.constants.AuthConstants;
import com.pikume.back.global.dto.CookieSpec;
import com.pikume.back.security.application.dto.AuthUserView;
import com.pikume.back.security.application.dto.AuthenticatedUserInfo;
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.dto.ReissueResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.request.LoginRequest;
import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.security.application.port.in.LoginUseCase;
import com.pikume.back.security.application.port.in.ReissueTokenUseCase;
import com.pikume.back.security.application.port.out.DeleteRefreshTokenPort;
import com.pikume.back.security.application.port.out.LoadRefreshTokenPort;
import com.pikume.back.security.application.port.out.LoadUserForAuthPort;
import com.pikume.back.security.application.port.out.SaveRefreshTokenPort;
import com.pikume.back.security.domain.RefreshToken;
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService implements LoginUseCase, ReissueTokenUseCase {

	private final LoadUserForAuthPort loadUserForAuthPort;
	private final LoadRefreshTokenPort loadRefreshTokenPort;
	private final SaveRefreshTokenPort saveRefreshTokenPort;
	private final DeleteRefreshTokenPort deleteRefreshTokenPort;
	private final JwtProvider jwtProvider;
	private final PasswordEncoder passwordEncoder;

	@Override
	public LoginResult login(LoginRequest dto, String deviceId) {
		log.info("event=login_requested outcome=accepted");

		AuthUserView user = loadUserForAuthPort.findByEmail(dto.getEmail())
				.orElseThrow(this::invalidCredentials);

		validateLoginPassword(dto.getPassword(), user.password());

		String accessToken = jwtProvider.generateAccessToken(user.id());
		String refreshToken = saveNewRefreshToken(user.id(), deviceId);

		log.info("event=login_completed outcome=success userId={}", user.id());
		return new LoginResult(
				new TokenDto(accessToken, refreshToken),
				new AuthenticatedUserInfo(
						user.id(),
						user.email(),
						user.nickname(),
						user.avatarPath()));
	}

	@Override
	@Transactional
	public String reissueAccessToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return null;
		}

		if (!jwtProvider.validateToken(refreshToken)) {
			deleteRefreshTokenPort.deleteByRefreshToken(refreshToken);
			return null;
		}

		RefreshToken tokenEntity = loadRefreshTokenPort.findByRefreshToken(refreshToken)
				.orElse(null);
		if (tokenEntity == null) {
			return null;
		}

		return generateAccessTokenForStoredRefreshToken(refreshToken, tokenEntity);
	}

	@Override
	@Transactional
	public ReissueResult reissueTokens(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return null;
		}

		if (!jwtProvider.validateToken(refreshToken)) {
			deleteRefreshTokenPort.deleteByRefreshToken(refreshToken);
			return null;
		}

		RefreshToken tokenEntity = loadRefreshTokenPort.findByRefreshToken(refreshToken)
				.orElse(null);
		if (tokenEntity == null) {
			return null;
		}

		String newAccessToken = generateAccessTokenForStoredRefreshToken(refreshToken, tokenEntity);
		if (newAccessToken == null) {
			return null;
		}

		return new ReissueResult(
				newAccessToken,
				refreshToken,
				AuthConstants.ACCESS_TOKEN_EXPIRATION_TIME / 1000L,
				AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME / 1000L);
	}

	@Override
	public CookieSpec newCookieRefreshToken(String refreshToken) {
		return new CookieSpec(
				AuthConstants.REFRESH_TOKEN,
				refreshToken,
				true,
				true,
				"/",
				AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME,
				"Lax");
	}

	@Override
	public CookieSpec removeCookieRefreshToken() {
		return new CookieSpec(
				AuthConstants.REFRESH_TOKEN,
				"",
				true,
				true,
				"/",
				0,
				"Lax");
	}

	@Override
	public void logout(String userId, String deviceId) {
		String key = userId + "-" + deviceId;
		deleteRefreshTokenPort.deleteById(key);
		log.info("event=logout_completed outcome=success userId={}", userId);
	}

	@Override
	@Transactional
	public void logoutByRefreshToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return;
		}
		deleteRefreshTokenPort.deleteByRefreshToken(refreshToken);
	}

	private void validateLoginPassword(String requestPassword, String storedPassword) {
		if (!passwordEncoder.matches(requestPassword, storedPassword)) {
			throw invalidCredentials();
		}
	}

	private InvalidCredentialsException invalidCredentials() {
		return new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	private String saveNewRefreshToken(String userId, String deviceId) {
		String key = userId + "-" + deviceId;
		String newRefreshToken = jwtProvider.generateRefreshToken();
		RefreshToken refreshTokenEntity = new RefreshToken(key, newRefreshToken, userId);
		saveRefreshTokenPort.save(refreshTokenEntity);
		log.debug("event=refresh_token_saved userId={}", userId);
		return newRefreshToken;
	}

	private String generateAccessTokenForStoredRefreshToken(String refreshToken, RefreshToken tokenEntity) {
		if (!StringUtils.hasText(tokenEntity.getUserId())) {
			deleteRefreshTokenPort.deleteByRefreshToken(refreshToken);
			log.warn("event=token_reissue_failed outcome=denied reason=missing_user_id");
			return null;
		}

		return jwtProvider.generateAccessToken(tokenEntity.getUserId());
	}
}
