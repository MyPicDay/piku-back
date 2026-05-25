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
		log.info("[Emitter 생성 요청]");
		String emitterId = userId + "_" + System.currentTimeMillis();
		notificationStreamPort.save(emitterId, userId, connection);

		connection.onCompletion(() -> {
			log.info("[Emitter 종료 - Completion] emitterId: {}", emitterId);
			notificationStreamPort.deleteById(emitterId);
		});

		connection.onError(error -> {
			log.debug("[Emitter 종료 - Error] emitterId: {}, cause: {}", emitterId, error.getMessage());
			notificationStreamPort.deleteById(emitterId);
		});

		connection.onTimeout(() -> {
			log.warn("[Emitter 종료 - Timeout] emitterId: {}", emitterId);
			notificationStreamPort.deleteById(emitterId);
			connection.complete();
		});

		long unreadCount = loadNotificationPort.countUnreadByReceiverId(userId);
		String eventId = userId + "_" + System.currentTimeMillis();
		if (!send(connection, emitterId, new NotificationStreamMessage(eventId, null, unreadCount))) {
			return;
		}

		boolean hasFriendRequest = loadNotificationPort.existsFriendRequestByReceiverId(userId);
		if (hasFriendRequest) {
			String friendEventId = userId + "_" + System.currentTimeMillis();
			log.info("[친구 요청 알림 전송] userId={}, eventId={}", userId, friendEventId);
			send(connection, emitterId, new NotificationStreamMessage(friendEventId, "FriendRequest", "on"));
		}
	}

	private boolean send(NotificationStreamConnection connection, String emitterId, NotificationStreamMessage message) {
		try {
			connection.send(message);
			return true;
		} catch (NotificationStreamSendException e) {
			log.debug("[Emitter 전송 실패] emitterId: {}, cause: {}", emitterId, e.getMessage());
			notificationStreamPort.deleteById(emitterId);
			return false;
		} catch (RuntimeException e) {
			notificationStreamPort.deleteById(emitterId);
			throw e;
		}
	}
}
