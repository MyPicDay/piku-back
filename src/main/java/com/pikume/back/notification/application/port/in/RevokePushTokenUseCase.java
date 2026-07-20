package com.pikume.back.notification.application.port.in;

public interface RevokePushTokenUseCase {

	void revokePushTokenForDevice(String userId, String deviceId);
}
