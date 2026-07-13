package com.pikume.back.user.auth.application.port.out;

public interface AuthenticationTokenPort {
	String generateAccessToken(String userId);
	String generateRefreshToken();
	boolean isTokenValid(String token);
	long accessTokenExpiresInSeconds();
	long refreshTokenExpiresInSeconds();
}
