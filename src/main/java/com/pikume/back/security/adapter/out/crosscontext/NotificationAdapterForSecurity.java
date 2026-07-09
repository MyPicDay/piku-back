package com.pikume.back.security.adapter.out.crosscontext;

import com.pikume.back.notification.application.port.in.FcmTokenUseCase;
import com.pikume.back.security.application.port.out.RevokeDevicePushTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationAdapterForSecurity implements RevokeDevicePushTokenPort {

	private final FcmTokenUseCase fcmTokenUseCase;

	@Override
	public void revokeDevicePushToken(String userId, String deviceId) {
		fcmTokenUseCase.revokeTokenForDevice(userId, deviceId);
	}
}
