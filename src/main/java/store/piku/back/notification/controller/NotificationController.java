package store.piku.back.notification.controller;

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
import store.piku.back.notification.dto.NotificationDTO;
import store.piku.back.notification.dto.response.CommentNotificationDTO;
import store.piku.back.notification.service.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestMetaMapper requestMetaMapper;


    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return notificationService.subscribe(userDetails.getId(), lastEventId);
    }
    
    @GetMapping("/notifications")
    public ResponseEntity<List<CommentNotificationDTO>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        List<CommentNotificationDTO> notifications = notificationService.getNotifications(userDetails.getId(),requestMetaInfo);
        return ResponseEntity.ok(notifications);
    }


    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        if (notificationService.markAsRead(notificationId)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 수정 완료, 내용 없음
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 없음
        }
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (notificationService.deleteNotification(notificationId,userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 삭제 완료
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 없음
        }
    }





}
