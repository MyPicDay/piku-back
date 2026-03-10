package com.pikume.back.notification.adapter.in.web;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.port.out.NotificationStreamConnection;

import java.io.IOException;

public class SseEmitterConnection implements NotificationStreamConnection {

	private final SseEmitter emitter;

	public SseEmitterConnection(long timeoutMillis) {
		this.emitter = new SseEmitter(timeoutMillis);
	}

	public SseEmitter emitter() {
		return emitter;
	}

	@Override
	public void onCompletion(Runnable action) {
		emitter.onCompletion(action);
	}

	@Override
	public void onTimeout(Runnable action) {
		emitter.onTimeout(action);
	}

	@Override
	public void complete() {
		emitter.complete();
	}

	@Override
	public void send(NotificationStreamMessage message) {
		try {
			SseEmitter.SseEventBuilder builder = SseEmitter.event().id(message.eventId()).data(message.data());
			if (message.eventName() != null) {
				builder.name(message.eventName());
			}
			emitter.send(builder);
		} catch (IOException e) {
			throw new RuntimeException("SSE 전송 실패", e);
		}
	}
}
