package com.pikume.back.notification.application.port.out;

public interface RevokePushTokenPort {

	void revokePushToken(String token);

	void revokePushTokenForDevice(String userId, String deviceId);
}
