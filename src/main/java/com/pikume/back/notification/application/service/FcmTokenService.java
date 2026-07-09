package com.pikume.back.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.notification.application.port.in.FcmTokenUseCase;
import com.pikume.back.notification.application.port.out.PushNotificationPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService implements FcmTokenUseCase {

	private final PushNotificationPort pushNotificationPort;

	@Override
	public void saveToken(String userId, String token, String deviceId) {
		log.info("event=fcm_token_save_requested userId={}", userId);
		pushNotificationPort.saveToken(userId, token, deviceId);
	}

	@Override
	public void revokeTokenForDevice(String userId, String deviceId) {
		log.info("event=fcm_device_token_revoke_requested userId={}", userId);
		pushNotificationPort.deleteTokenForDevice(userId, deviceId);
	}
}
