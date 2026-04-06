package com.pikume.back.notification.adapter.in.web;

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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.global.pagination.SortQuery;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.notification.adapter.in.web.dto.NotificationResponseDTO;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.port.in.NotificationUseCase;
import com.pikume.back.notification.application.port.in.SseUseCase;

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
	private final ProblemDetailFactory problemDetailFactory;
	private static final long DEFAULT_SSE_TIMEOUT = 60L * 1000 * 60;

	@Operation(summary = "SSE 구독 시작", description = "서버-전송 이벤트 연결")
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
		String userId = userDetails.getId();
		log.info("SSE 구독 요청 - userId: {}", userId);
		SseEmitterConnection connection = new SseEmitterConnection(DEFAULT_SSE_TIMEOUT);
		sseUseCase.subscribe(userId, connection);
		return connection.emitter();
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
		PageQuery pageQuery = new PageQuery(
				sortedPageable.getPageNumber(),
				sortedPageable.getPageSize(),
				java.util.List.of(SortQuery.desc("createdAt")));
		PageResult<NotificationResponseDTO> notificationResults = notificationUseCase.getNotifications(
				userDetails.getId(), requestMetaInfo, pageQuery)
				.map(this::toResponseDto);
		Page<NotificationResponseDTO> notifications = SpringPageMapper.toSpringPage(notificationResults, sortedPageable);
		return ResponseEntity.ok(notifications);
	}

	@Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 표시합니다.")
	@PatchMapping("/{notificationId}")
	public ResponseEntity<?> markAsRead(@PathVariable Long notificationId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		if (notificationUseCase.markAsRead(notificationId, userDetails.getId())) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return notFoundProblem("/api/sse/" + notificationId, "알림을 찾을 수 없습니다.");
	}

	@Operation(summary = "알림 모두 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 표시합니다.")
	@PatchMapping("/notifications")
	public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
		notificationUseCase.markAllAsRead(userDetails.getId());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.(SoftDelete)")
	@DeleteMapping("/{notificationId}")
	public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		if (notificationUseCase.deleteNotification(notificationId, userDetails.getId())) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return notFoundProblem("/api/sse/" + notificationId, "알림을 찾을 수 없습니다.");
	}

	private ResponseEntity<ProblemDetail> notFoundProblem(String instance, String detail) {
		ProblemDetail problemDetail = problemDetailFactory.create(CommonProblemType.RESOURCE_NOT_FOUND, detail, instance);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	private NotificationResponseDTO toResponseDto(NotificationResult notification) {
		return new NotificationResponseDTO(
				notification.id(),
				notification.message(),
				notification.nickname(),
				notification.avatarUrl(),
				notification.type(),
				notification.relatedDiaryId(),
				notification.thumbnailUrl(),
				notification.isRead(),
				notification.createdAt(),
				notification.diaryDate(),
				notification.diaryUserId());
	}
}
