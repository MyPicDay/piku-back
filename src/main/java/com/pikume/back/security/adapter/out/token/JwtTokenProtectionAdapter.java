package com.pikume.back.security.adapter.out.token;

import com.pikume.back.security.jwt.JwtProvider;
import com.pikume.back.user.auth.application.port.out.AuthenticationTokenPort;
import com.pikume.back.security.config.UserTokenSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProtectionAdapter implements AuthenticationTokenPort {
	private final JwtProvider jwtProvider;
	public String generateAccessToken(String userId) { return jwtProvider.generateAccessToken(userId); }
	public String generateRefreshToken() { return jwtProvider.generateRefreshToken(); }
	public boolean isTokenValid(String token) { return jwtProvider.validateToken(token); }
	public long accessTokenExpiresInSeconds() { return UserTokenSettings.ACCESS_TOKEN_EXPIRATION_MILLIS / 1000L; }
	public long refreshTokenExpiresInSeconds() { return UserTokenSettings.REFRESH_TOKEN_EXPIRATION_MILLIS / 1000L; }
}
