package store.piku.back.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.notification.dto.NotificationDTO;
import store.piku.back.notification.entity.Notification;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.repository.EmitterRepository;
import store.piku.back.notification.repository.NotificationRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.reader.UserReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Long DEFAULT_TIMEOUT = 60L*1000*60;

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final UserReader userReader;


    // 구독 메서드
    public SseEmitter subscribe(String userId, String lastEventId) {
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        String eventId = userId + "_" + System.currentTimeMillis();
        sendToClient(emitter, eventId,"EventStream Created. [userId=" + userId +"]");

        if (!lastEventId.isEmpty()) {
            sendLostData(lastEventId, userId, emitterId, emitter);
        }
        return emitter;
    }

    public void sendToClient(SseEmitter emitter, String eventId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .data(data));
        } catch (IOException exception) {
            emitterRepository.deleteById(eventId);
            throw new RuntimeException("연결 오류!");
        }
    }

    @Transactional
    public void send(User receiver, NotificationType notificationType, String content, String url) {
        Notification notification = notificationRepository.save(createNotification(receiver, notificationType, content, url));

        String receiverId = String.valueOf(receiver.getId());
        String eventId = receiverId + "_" + System.currentTimeMillis();
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(receiverId);
        emitters.forEach(
                (key, emitter) -> {
                    emitterRepository.saveEventCache(key, notification); // 놓쳤을 때 캐시에 저장
                    sendToClient(emitter, eventId, notification);
                }
        );
    }

    private Notification createNotification(User receiver, NotificationType notificationType, String content, String url) {

        if(notificationType.equals(NotificationType.FRIEND)) {
            return Notification.builder()
                    .receiver(receiver)
                    .notificationType(notificationType)
                    .content(content)
                    .url(url)
                    .isRead(false)
                    .build();
        }else if(notificationType.equals(NotificationType.COMMENT)) {
            return  Notification.builder()
                    .receiver(receiver)
                    .notificationType(notificationType)
                    .content(content)
                    .url(url)
                    .isRead(false)
                    .build();
        }
       return null;
    }

    public void sendNotification(SseEmitter emitter, String eventId, String emitterId) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(eventId)         // 이벤트 고유 ID
                    .name("message")  ;   // 이벤트 이름 (필요시 사용)
                            // 전송할 메시지 데이터

            emitter.send(event);
        } catch (IOException e) {
            // 전송 실패 시 emitter 제거 등 예외 처리
            emitterRepository.deleteById(emitterId);
            e.printStackTrace();
        }
    }

    @Transactional
    public void saveNotification(String receiverId, NotificationType type, String nickname, String relatedId  ) {
        String message = nickname + "님이 회원님의 게시글에 댓글을 남겼습니다.";

        Notification notification = new Notification(
                null,
                receiverId,
                type,
                message,
                false,         // isRead 기본값 false
                relatedId
        );

        notificationRepository.save(notification);
    }




    private void sendLostData(String lastEventId, String userId, String emitterId, SseEmitter emitter) {
        Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByUserId(String.valueOf(userId));
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
    }


    public List<NotificationDTO> getNotifications(String userId) {
        User user= userReader.getUserById(userId);
        List<NotificationDTO> notifications = notificationRepository.findAllByUser(user);
    }




}
