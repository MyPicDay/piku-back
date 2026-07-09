package com.pikume.back.security.application.port.out;

public interface RevokeDevicePushTokenPort {

	void revokeDevicePushToken(String userId, String deviceId);
}
