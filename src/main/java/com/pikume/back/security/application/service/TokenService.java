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
import com.pikume.back.security.application.dto.LoginResult;
import com.pikume.back.security.application.exception.InvalidCredentialsException;
import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.UserInfo;
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
		log.info("[로그인] 서비스 호출 : 이메일={}", dto.getEmail());

		AuthUserView user = loadUserForAuthPort.findByEmail(dto.getEmail())
				.orElseThrow(this::invalidCredentials);

		validateLoginPassword(dto.getPassword(), user.password(), dto.getEmail());

		String accessToken = jwtProvider.generateAccessToken(dto.getEmail());
		String refreshToken = saveNewRefreshToken(dto.getEmail(), deviceId, user.id());

		log.info("[로그인] 완료 : 이메일={}", dto.getEmail());
		return new LoginResult(
				new TokenDto(accessToken, refreshToken),
				new UserInfo(user.id(), user.email(), user.nickname(), user.avatarPath()));
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

		String email = tokenEntity.getKey().split("-")[0];
		return jwtProvider.generateAccessToken(email);
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
	public void logout(String email, String deviceId) {
		String key = email + "-" + deviceId;
		deleteRefreshTokenPort.deleteById(key);
		log.info("[로그아웃] Refresh Token 삭제 완료 : key={}", key);
	}

	private void validateLoginPassword(String requestPassword, String storedPassword, String email) {
		if (!passwordEncoder.matches(requestPassword, storedPassword)) {
			log.warn("[로그인] 실패 - 비밀번호 불일치 : 이메일={}", email);
			throw invalidCredentials();
		}
	}

	private InvalidCredentialsException invalidCredentials() {
		return new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	private String saveNewRefreshToken(String email, String deviceId, String userId) {
		String key = email + "-" + deviceId;
		String newRefreshToken = jwtProvider.generateRefreshToken();
		RefreshToken refreshTokenEntity = new RefreshToken(key, newRefreshToken, userId);
		saveRefreshTokenPort.save(refreshTokenEntity);
		log.info("[JWT Refresh Token 저장 완료] key={}", key);
		return newRefreshToken;
	}
}
