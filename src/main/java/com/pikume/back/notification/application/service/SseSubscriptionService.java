package com.pikume.back.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.exception.NotificationStreamSendException;
import com.pikume.back.notification.application.port.in.SseUseCase;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;
import com.pikume.back.notification.application.port.out.NotificationStreamPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseSubscriptionService implements SseUseCase {

	private final NotificationStreamPort notificationStreamPort;
	private final LoadNotificationPort loadNotificationPort;

	@Override
	public void subscribe(String userId, NotificationStreamConnection connection) {
		log.info("event=sse_subscription_requested outcome=accepted userId={}", userId);
		String emitterId = userId + "_" + System.currentTimeMillis();
		notificationStreamPort.save(emitterId, userId, connection);

		connection.onCompletion(() -> {
			log.info("event=sse_connection_completed outcome=success userId={} resourceId={}",
					userId, emitterId);
			notificationStreamPort.delete(userId, emitterId);
		});

		connection.onError(error -> {
			log.warn("event=sse_connection_closed outcome=failed userId={} resourceId={} "
					+ "reason=connection_error exception={}",
					userId, emitterId, error.getClass().getSimpleName());
			notificationStreamPort.delete(userId, emitterId);
		});

		connection.onTimeout(() -> {
			log.warn("event=sse_connection_closed outcome=failed userId={} resourceId={} reason=timeout",
					userId, emitterId);
			notificationStreamPort.delete(userId, emitterId);
			connection.complete();
		});

		long unreadCount = loadNotificationPort.countUnreadByReceiverId(userId);
		String eventId = userId + "_" + System.currentTimeMillis();
		if (!send(userId, emitterId, connection, new NotificationStreamMessage(eventId, null, unreadCount))) {
			return;
		}

		boolean hasFriendRequest = loadNotificationPort.existsFriendRequestByReceiverId(userId);
		if (hasFriendRequest) {
			String friendEventId = userId + "_" + System.currentTimeMillis();
			log.info("event=sse_friend_request_notification_send_requested outcome=accepted "
					+ "userId={} resourceId={}", userId, friendEventId);
			send(userId, emitterId, connection,
					new NotificationStreamMessage(friendEventId, "FriendRequest", "on"));
		}
	}

	private boolean send(String userId, String emitterId, NotificationStreamConnection connection,
			NotificationStreamMessage message) {
		try {
			connection.send(message);
			return true;
		} catch (NotificationStreamSendException e) {
			log.warn("event=sse_notification_delivery outcome=failed userId={} resourceId={} "
					+ "reason=stream_send_failed exception={}",
					userId, emitterId, e.getClass().getSimpleName());
			notificationStreamPort.delete(userId, emitterId);
			return false;
		} catch (RuntimeException e) {
			notificationStreamPort.delete(userId, emitterId);
			throw e;
		}
	}
}
