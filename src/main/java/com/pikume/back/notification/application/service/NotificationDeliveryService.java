package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;
import com.pikume.back.notification.application.port.in.DeliverNotificationUseCase;
import com.pikume.back.notification.application.port.out.DeliverNotificationStreamPort;
import com.pikume.back.notification.application.port.out.DeliverPushNotificationPort;
import com.pikume.back.notification.application.port.out.LoadPushDeliveryTokensPort;
import com.pikume.back.notification.application.port.out.RevokePushTokenPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService implements DeliverNotificationUseCase {

	private final DeliverNotificationStreamPort deliverNotificationStreamPort;
	private final LoadPushDeliveryTokensPort loadPushDeliveryTokensPort;
	private final DeliverPushNotificationPort deliverPushNotificationPort;
	private final RevokePushTokenPort revokePushTokenPort;

	@Override
	public void deliverNotification(NotificationDeliveryRequest request) {
		deliverStream(request);
		deliverPush(request);
	}

	private void deliverStream(NotificationDeliveryRequest request) {
		try {
			deliverNotificationStreamPort.deliverNotificationStream(
					request.receiverId(),
					request.streamMessage());
		} catch (RuntimeException e) {
			log.warn("event=sse_notification_delivery outcome=failed userId={} exception={}",
					request.receiverId(), e.getClass().getSimpleName());
		}
	}

	private void deliverPush(NotificationDeliveryRequest request) {
		Set<String> tokens;
		try {
			tokens = loadPushDeliveryTokensPort.loadPushDeliveryTokens(request.receiverId());
		} catch (RuntimeException e) {
			log.warn("event=fcm_notification_delivery outcome=failed userId={} reason=token_load_failed exception={}",
					request.receiverId(), e.getClass().getSimpleName());
			return;
		}
		if (tokens.isEmpty()) {
			log.warn("event=fcm_token_missing outcome=skipped userId={}", request.receiverId());
			return;
		}
		for (String token : tokens) {
			try {
				deliverPushNotificationPort.deliverPushNotification(token, request.pushBody());
			} catch (RuntimeException e) {
				log.debug("event=fcm_notification_send_failed outcome=failed reason={}",
						e.getClass().getSimpleName());
				revokeFailedToken(token);
			}
		}
	}

	private void revokeFailedToken(String token) {
		try {
			revokePushTokenPort.revokePushToken(token);
		} catch (RuntimeException e) {
			log.warn("event=fcm_token_revoke_failed outcome=failed exception={}",
					e.getClass().getSimpleName());
		}
	}
}
