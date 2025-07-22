package store.piku.back.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;
import store.piku.back.notification.dto.response.NotificationResponseDTO;
import store.piku.back.notification.service.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestMetaMapper requestMetaMapper;

    @Operation(summary = "SSE 구독 시작", description = "서버-전송 이벤트 연결")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return notificationService.subscribe(userDetails.getId(), lastEventId);
    }

    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 조회합니다.")
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        List<NotificationResponseDTO> notifications = notificationService.getNotifications(userDetails.getId(),requestMetaInfo);
        return ResponseEntity.ok(notifications);
    }


    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 클릭하면 읽음 상태로 표시합니다.")
    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId,@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (notificationService.markAsRead(notificationId,userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.(SoftDelete)")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (notificationService.deleteNotification(notificationId,userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }





}
