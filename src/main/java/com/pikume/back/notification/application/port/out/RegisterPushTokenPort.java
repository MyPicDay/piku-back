package com.pikume.back.notification.application.port.out;

public interface RegisterPushTokenPort {

	void registerPushToken(String userId, String token, String deviceId);
}
