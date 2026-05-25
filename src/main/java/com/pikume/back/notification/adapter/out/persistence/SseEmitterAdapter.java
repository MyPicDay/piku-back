package com.pikume.back.notification.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.exception.NotificationStreamSendException;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;
import com.pikume.back.notification.application.port.out.NotificationStreamPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseEmitterAdapter implements NotificationStreamPort {

	private final Map<String, EmitterConnection> emitters = new ConcurrentHashMap<>();

	@Override
	public void save(String emitterId, String userId, NotificationStreamConnection connection) {
		emitters.put(emitterId, new EmitterConnection(userId, connection));
	}

	@Override
	public void sendToUser(String userId, NotificationStreamMessage message) {
		emitters.forEach((emitterId, emitter) -> {
			if (!emitter.userId().equals(userId)) {
				return;
			}
			try {
				log.info("[SSE 알림 전송] userId: {}, emitterId: {}", userId, emitterId);
				emitter.connection().send(message);
			} catch (NotificationStreamSendException e) {
				log.warn("SSE 알림 전송 실패: {}", e.getMessage());
				deleteById(emitterId);
			} catch (RuntimeException e) {
				deleteById(emitterId);
				throw e;
			}
		});
	}

	@Override
	public void deleteById(String emitterId) {
		emitters.remove(emitterId);
	}

	private record EmitterConnection(String userId, NotificationStreamConnection connection) {
	}
}
