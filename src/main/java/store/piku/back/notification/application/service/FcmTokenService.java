package store.piku.back.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.piku.back.notification.application.port.in.FcmTokenUseCase;
import store.piku.back.notification.application.port.out.PushNotificationPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService implements FcmTokenUseCase {

	private final PushNotificationPort pushNotificationPort;

	@Override
	public void saveToken(String userId, String token, String deviceId) {
		log.info("FCM 토큰 저장 - userId: {}, deviceId: {}", userId, deviceId);
		pushNotificationPort.saveToken(userId, token, deviceId);
	}
}
