package com.pikume.back.user.auth.application.port.out;

public interface RevokeDevicePushTokenPort {
	void revokeDevicePushToken(String userId, String deviceId);
}
