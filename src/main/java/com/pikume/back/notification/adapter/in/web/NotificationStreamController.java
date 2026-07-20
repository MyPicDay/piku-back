package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.notification.application.port.in.SubscribeNotificationStreamUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationStreamController {

	private static final long DEFAULT_SSE_TIMEOUT = 60L * 1000 * 60;

	private final SubscribeNotificationStreamUseCase subscribeNotificationStreamUseCase;

	@Operation(summary = "SSE 구독 시작", description = "서버-전송 이벤트 연결")
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
		SseEmitterConnection connection = new SseEmitterConnection(DEFAULT_SSE_TIMEOUT);
		subscribeNotificationStreamUseCase.subscribeToNotifications(userDetails.getId(), connection);
		return connection.emitter();
	}
}
