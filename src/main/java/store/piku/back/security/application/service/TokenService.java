package store.piku.back.security.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import store.piku.back.auth.constants.AuthConstants;
import store.piku.back.auth.dto.TokenDto;
import store.piku.back.auth.dto.UserInfo;
import store.piku.back.auth.dto.request.LoginRequest;
import store.piku.back.auth.jwt.JwtProvider;
import store.piku.back.security.application.port.in.LoginUseCase;
import store.piku.back.security.application.port.in.ReissueTokenUseCase;
import store.piku.back.security.application.port.out.DeleteRefreshTokenPort;
import store.piku.back.security.application.port.out.LoadRefreshTokenPort;
import store.piku.back.security.application.port.out.LoadUserForAuthPort;
import store.piku.back.security.application.port.out.SaveRefreshTokenPort;
import store.piku.back.security.domain.RefreshToken;
import store.piku.back.user.domain.User;

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
	public TokenDto login(LoginRequest dto, String deviceId) {
		log.info("[로그인] 서비스 호출 : 이메일={}", dto.getEmail());

		User user = loadUserForAuthPort.findByEmail(dto.getEmail())
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		validateLoginPassword(dto.getPassword(), user.getPassword(), dto.getEmail());

		String accessToken = jwtProvider.generateAccessToken(dto.getEmail());
		String refreshToken = saveNewRefreshToken(dto.getEmail(), deviceId, user.getId());

		log.info("[로그인] 완료 : 이메일={}", dto.getEmail());
		return new TokenDto(accessToken, refreshToken);
	}

	@Override
	public UserInfo getUserInfoByEmail(String email) {
		User user = loadUserForAuthPort.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		return new UserInfo(
				String.valueOf(user.getId()),
				user.getEmail(),
				user.getNickname(),
				user.getAvatar());
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
				.orElseThrow(() -> new RuntimeException("저장된 리프레시 토큰 없음"));

		String email = tokenEntity.getKey().split("-")[0];
		return jwtProvider.generateAccessToken(email);
	}

	@Override
	public ResponseCookie newCookieRefreshToken(String refreshToken) {
		return ResponseCookie.from(AuthConstants.REFRESH_TOKEN, refreshToken)
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(AuthConstants.REFRESH_TOKEN_EXPIRATION_TIME)
				.sameSite("Lax")
				.build();
	}

	@Override
	public ResponseCookie removeCookieRefreshToken() {
		return ResponseCookie.from(AuthConstants.REFRESH_TOKEN, "")
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(0)
				.sameSite("Lax")
				.build();
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
			throw new RuntimeException("비밀번호가 일치하지 않습니다.");
		}
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
