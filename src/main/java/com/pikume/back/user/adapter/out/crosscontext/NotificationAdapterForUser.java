package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.notification.application.port.in.RevokePushTokenUseCase;
import com.pikume.back.user.auth.application.port.out.RevokeDevicePushTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationAdapterForUser implements RevokeDevicePushTokenPort {
	private final RevokePushTokenUseCase revokePushTokenUseCase;

	@Override
	public void revokeDevicePushToken(String userId, String deviceId) {
		revokePushTokenUseCase.revokePushTokenForDevice(userId, deviceId);
	}
}
