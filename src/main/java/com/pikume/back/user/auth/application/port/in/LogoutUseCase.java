package com.pikume.back.user.auth.application.port.in;

public interface LogoutUseCase {
	void logout(String userId, String deviceId);
	void logoutWithRefreshToken(String refreshToken, String deviceId);
}
