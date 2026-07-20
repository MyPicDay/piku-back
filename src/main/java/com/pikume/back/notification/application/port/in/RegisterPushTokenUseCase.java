package com.pikume.back.notification.application.port.in;

public interface RegisterPushTokenUseCase {

	void registerPushToken(String userId, String token, String deviceId);
}
