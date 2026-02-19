package store.piku.back.notification.adapter.out.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import store.piku.back.notification.application.port.out.PushNotificationPort;

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
		log.info("[Local] 토큰 삭제: {}", token);
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
