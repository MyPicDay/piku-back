package com.pikume.back.notification.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.exception.NotificationStreamSendException;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;
import com.pikume.back.notification.application.port.out.NotificationStreamPort;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class SseEmitterAdapter implements NotificationStreamPort {

	private final ConcurrentMap<String, ConcurrentMap<String, NotificationStreamConnection>> connectionsByUserId =
			new ConcurrentHashMap<>();

	@Override
	public void save(String emitterId, String userId, NotificationStreamConnection connection) {
		connectionsByUserId.compute(userId, (key, connections) -> {
			ConcurrentMap<String, NotificationStreamConnection> userConnections = connections == null
					? new ConcurrentHashMap<>()
					: connections;
			userConnections.put(emitterId, connection);
			return userConnections;
		});
	}

	@Override
	public void sendToUser(String userId, NotificationStreamMessage message) {
		ConcurrentMap<String, NotificationStreamConnection> userConnections = connectionsByUserId.get(userId);
		if (userConnections == null) {
			return;
		}
		userConnections.forEach((emitterId, connection) -> {
			try {
				log.info("[SSE 알림 전송] userId: {}, emitterId: {}", userId, emitterId);
				connection.send(message);
			} catch (NotificationStreamSendException e) {
				log.warn("SSE 알림 전송 실패: {}", e.getMessage());
				delete(userId, emitterId);
			} catch (RuntimeException e) {
				delete(userId, emitterId);
				throw e;
			}
		});
	}

	@Override
	public void delete(String userId, String emitterId) {
		connectionsByUserId.computeIfPresent(userId, (key, connections) -> {
			connections.remove(emitterId);
			return connections.isEmpty() ? null : connections;
		});
	}
}
