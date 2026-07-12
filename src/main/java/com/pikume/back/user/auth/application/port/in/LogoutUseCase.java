package com.pikume.back.user.auth.application.port.in;

public interface LogoutUseCase {
	void logout(String userId, String deviceId);
	void logoutByRefreshToken(String refreshToken, String deviceId);
}
