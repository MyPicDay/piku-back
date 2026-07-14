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
				log.info("event=sse_notification_send_requested outcome=accepted userId={} resourceId={}",
						userId, emitterId);
				connection.send(message);
			} catch (NotificationStreamSendException e) {
				log.warn("event=sse_notification_delivery outcome=failed userId={} resourceId={} "
						+ "reason=stream_send_failed exception={}",
						userId, emitterId, e.getClass().getSimpleName());
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
