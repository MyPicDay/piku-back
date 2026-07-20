package com.pikume.back.notification.adapter.out.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.exception.PushNotificationDeliveryException;
import com.pikume.back.notification.application.port.out.DeliverPushNotificationPort;

@Component
@Profile("prod")
public class FcmPushAdapter implements DeliverPushNotificationPort {

	private void sendMessage(String targetToken, String body) throws FirebaseMessagingException {
		Message message = Message.builder()
				.setToken(targetToken)
				.putData("title", "PikU 알림")
				.putData("body", body)
				.putData("url", "/notifications")
				.build();

		FirebaseMessaging.getInstance().send(message);
	}

	@Override
	public void deliverPushNotification(String targetToken, String body) {
		try {
			sendMessage(targetToken, body);
		} catch (FirebaseMessagingException e) {
			throw new PushNotificationDeliveryException("FCM 알림 전송 실패", e);
		}
	}
}
