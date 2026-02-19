package store.piku.back.notification.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseUseCase {

	SseEmitter subscribe(String userId);
}
