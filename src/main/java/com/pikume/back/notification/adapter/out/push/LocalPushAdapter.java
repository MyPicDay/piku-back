package com.pikume.back.notification.adapter.out.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.port.out.DeliverPushNotificationPort;

@Component
@Profile("!prod")
@Slf4j
public class LocalPushAdapter implements DeliverPushNotificationPort {

	@Override
	public void deliverPushNotification(String targetToken, String body) {
		log.info("[Local] Firebase 알림 전송(가정): {}", body);
	}
}
