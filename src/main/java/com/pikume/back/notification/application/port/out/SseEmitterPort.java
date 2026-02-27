package com.pikume.back.notification.application.port.out;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface SseEmitterPort {

	SseEmitter save(String emitterId, SseEmitter sseEmitter);

	Map<String, SseEmitter> findAllByUserId(String userId);

	void deleteById(String emitterId);
}
