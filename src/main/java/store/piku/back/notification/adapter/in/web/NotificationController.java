package store.piku.back.notification.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;
import store.piku.back.notification.adapter.in.web.dto.NotificationResponseDTO;
import store.piku.back.notification.application.port.in.NotificationUseCase;
import store.piku.back.notification.application.port.in.SseUseCase;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
@Slf4j
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

	private final NotificationUseCase notificationUseCase;
	private final SseUseCase sseUseCase;
	private final RequestMetaMapper requestMetaMapper;

	@Operation(summary = "SSE 구독 시작", description = "서버-전송 이벤트 연결")
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
		String userId = userDetails.getId();
		log.info("SSE 구독 요청 - userId: {}", userId);
		return sseUseCase.subscribe(userId);
	}

	@Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 조회합니다.")
	@GetMapping("/notifications")
	public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PageableDefault Pageable pageable,
			HttpServletRequest request) {
		Pageable sortedPageable = PageRequest.of(
				pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		Page<NotificationResponseDTO> notifications = notificationUseCase.getNotifications(
				userDetails.getId(), requestMetaInfo, sortedPageable);
		return ResponseEntity.ok(notifications);
	}

	@Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 표시합니다.")
	@PatchMapping("/{notificationId}")
	public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		if (notificationUseCase.markAsRead(notificationId, userDetails.getId())) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}

	@Operation(summary = "알림 모두 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 표시합니다.")
	@PatchMapping("/notifications")
	public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
		notificationUseCase.markAllAsRead(userDetails.getId());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.(SoftDelete)")
	@DeleteMapping("/{notificationId}")
	public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		if (notificationUseCase.deleteNotification(notificationId, userDetails.getId())) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}
}
