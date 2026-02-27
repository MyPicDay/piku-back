package com.pikume.back.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pikume.back.notification.application.port.in.SseUseCase;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.SseEmitterPort;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseSubscriptionService implements SseUseCase {

	// private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
	private static final Long DEFAULT_TIMEOUT = 60L * 1000;

	private final SseEmitterPort sseEmitterPort;
	private final LoadNotificationPort loadNotificationPort;

	@Override
	public SseEmitter subscribe(String userId) {
		log.info("[Emitter 생성 요청]");
		String emitterId = userId + "_" + System.currentTimeMillis();
		SseEmitter emitter = sseEmitterPort.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

		emitter.onCompletion(() -> {
			log.info("[Emitter 종료 - Completion] emitterId: {}", emitterId);
			sseEmitterPort.deleteById(emitterId);
		});

		emitter.onTimeout(() -> {
			log.warn("[Emitter 종료 - Timeout] emitterId: {}", emitterId);
			emitter.complete(); // DeferredResult에 정상 결과 설정 → onCompletion에서 정리
		});

		long unreadCount = loadNotificationPort.countUnreadByReceiverId(userId);
		String eventId = userId + "_" + System.currentTimeMillis();
		sendInitialCount(emitter, eventId, emitterId, unreadCount);

		boolean hasFriendRequest = loadNotificationPort.existsFriendRequestByReceiverId(userId);
		if (hasFriendRequest) {
			String friendEventId = userId + "_" + System.currentTimeMillis();
			try {
				log.info("[친구 요청 알림 전송] userId={}, eventId={}", userId, friendEventId);
				emitter.send(SseEmitter.event()
						.id(friendEventId)
						.name("FriendRequest")
						.data("on"));
			} catch (IOException e) {
				log.warn("친구 요청 전송 실패 → emitter 제거");
				sseEmitterPort.deleteById(emitterId);
			}
		}

		return emitter;
	}

	private void sendInitialCount(SseEmitter emitter, String eventId, String emitterId, Long count) {
		try {
			emitter.send(SseEmitter.event().id(eventId).data(count));
		} catch (IOException e) {
			sseEmitterPort.deleteById(emitterId);
			throw new RuntimeException("연결 오류!");
		}
	}
}
