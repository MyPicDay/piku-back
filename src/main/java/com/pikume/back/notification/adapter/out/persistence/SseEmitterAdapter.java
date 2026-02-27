package com.pikume.back.notification.adapter.out.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pikume.back.notification.application.port.out.SseEmitterPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SseEmitterAdapter implements SseEmitterPort {

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	@Override
	public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
		emitters.put(emitterId, sseEmitter);
		return sseEmitter;
	}

	@Override
	public Map<String, SseEmitter> findAllByUserId(String userId) {
		return emitters.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(userId))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public void deleteById(String emitterId) {
		emitters.remove(emitterId);
	}
}
