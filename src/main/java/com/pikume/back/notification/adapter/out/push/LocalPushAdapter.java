package com.pikume.back.notification.adapter.out.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.port.out.PushNotificationPort;

import java.util.Collections;
import java.util.Set;

@Component
@Profile("!prod")
@Slf4j
@RequiredArgsConstructor
public class LocalPushAdapter implements PushNotificationPort {

	@Override
	public Set<String> getTokenByUserId(String userId) {
		return Collections.emptySet();
	}

	@Override
	public void deleteToken(String token) {
		log.info("event=local_fcm_token_delete_requested outcome=accepted");
	}

	@Override
	public void deleteTokenForDevice(String userId, String deviceId) {
		log.info("event=local_fcm_device_token_delete_requested userId={}", userId);
	}

	@Override
	public void saveToken(String userId, String token, String deviceId) {
		log.info("[Local] FCM 토큰 저장(가정)");
	}

	@Override
	public void sendMessage(String targetToken, String body) {
		log.info("[Local] Firebase 알림 전송(가정): {}", body);
	}
}
