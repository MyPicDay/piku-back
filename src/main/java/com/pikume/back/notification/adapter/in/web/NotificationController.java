package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SortQuery;
import com.pikume.back.notification.adapter.in.web.dto.NotificationPageResponse;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.port.in.DeleteNotificationUseCase;
import com.pikume.back.notification.application.port.in.MarkAllNotificationsReadUseCase;
import com.pikume.back.notification.application.port.in.MarkNotificationReadUseCase;
import com.pikume.back.notification.application.port.in.QueryNotificationPageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

	private final QueryNotificationPageUseCase queryNotificationPageUseCase;
	private final MarkNotificationReadUseCase markNotificationReadUseCase;
	private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
	private final DeleteNotificationUseCase deleteNotificationUseCase;
	private final NotificationWebMapper notificationWebMapper;
	private final ProblemDetailFactory problemDetailFactory;

	@Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 조회합니다.")
	@GetMapping("/notifications")
	public ResponseEntity<NotificationPageResponse> getNotifications(
			@AuthenticationPrincipal UserPrincipal userDetails,
			@PageableDefault Pageable pageable) {
		Pageable sortedPageable = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		PageQuery pageQuery = new PageQuery(
				sortedPageable.getPageNumber(),
				sortedPageable.getPageSize(),
				java.util.List.of(SortQuery.desc("createdAt")));
		PageResult<NotificationResult> result =
				queryNotificationPageUseCase.queryNotifications(userDetails.getId(), pageQuery);
		return ResponseEntity.ok(notificationWebMapper.toPageResponse(result, sortedPageable));
	}

	@Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 표시합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
			@ApiResponse(
					responseCode = "404",
					description = "알림을 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PatchMapping("/{notificationId}")
	public ResponseEntity<?> markAsRead(
			@PathVariable Long notificationId,
			@AuthenticationPrincipal UserPrincipal userDetails) {
		if (markNotificationReadUseCase.markNotificationRead(notificationId, userDetails.getId())) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return notFoundProblem("/api/sse/" + notificationId, "알림을 찾을 수 없습니다.");
	}

	@Operation(summary = "알림 모두 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 표시합니다.")
	@PatchMapping("/notifications")
	public ResponseEntity<Void> markAllAsRead(
			@AuthenticationPrincipal UserPrincipal userDetails) {
		markAllNotificationsReadUseCase.markAllNotificationsRead(userDetails.getId());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.(SoftDelete)")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "삭제 성공"),
			@ApiResponse(
					responseCode = "404",
					description = "알림을 찾을 수 없음",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@DeleteMapping("/{notificationId}")
	public ResponseEntity<?> deleteNotification(
			@PathVariable Long notificationId,
			@AuthenticationPrincipal UserPrincipal userDetails) {
		if (deleteNotificationUseCase.deleteNotification(notificationId, userDetails.getId())) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		return notFoundProblem("/api/sse/" + notificationId, "알림을 찾을 수 없습니다.");
	}

	private ResponseEntity<ProblemDetail> notFoundProblem(String instance, String detail) {
		ProblemDetail problemDetail =
				problemDetailFactory.create(CommonProblemType.RESOURCE_NOT_FOUND, detail, instance);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problemDetail);
	}
}
