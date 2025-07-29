package store.piku.back.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.notification.dto.response.NotificationResponseDTO;
import store.piku.back.notification.entity.Notification;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.exception.NotificationNotFoundException;
import store.piku.back.notification.repository.EmitterRepository;
import store.piku.back.notification.repository.NotificationRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.reader.UserReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final Long DEFAULT_TIMEOUT = 60L*1000*60;

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final UserReader userReader;
    private final PhotoRepository photoRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;


    public SseEmitter subscribe(String userId, String lastEventId) {
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(userId);
        String eventId = userId + "_" + System.currentTimeMillis();

        emitterRepository.saveEventCache(emitterId, String.valueOf(unreadCount));;
        sendToClient(emitter, eventId, String.valueOf(unreadCount));

        if (!lastEventId.isEmpty()) {
            sendLostData(lastEventId, userId, emitterId, emitter);
        }
        return emitter;
    }

    private void sendLostData(String lastEventId, String userId, String emitterId, SseEmitter emitter) {
        Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByUserId(String.valueOf(userId));
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                .forEach(entry -> {
                    String unreadCount = (String) entry.getValue(); // 알림 개수 (문자열)
                    sendToClient(emitter, entry.getKey(), unreadCount);
                });
    }

    public void sendToClient(SseEmitter emitter, String eventId, String message) {
        try {
            emitter.send(SseEmitter.event().id(eventId).data(message));
        } catch (IOException e) {
            emitterRepository.deleteById(eventId);
            throw new RuntimeException("연결 오류!");
        }
    }




    @Transactional
    public void saveNotification(String receiverId, NotificationType type, String message, String relatedId) {

        log.info("알림 저장 요청 - receiverId: {}, type: {}, message: {}, relatedId: {}", receiverId, type, message, relatedId);
        Notification notification = new Notification(
                null,
                receiverId,
                type,
                message,
                false,
                relatedId
        );
        notificationRepository.save(notification);
    }


    public List<NotificationResponseDTO> getNotifications(String userId, RequestMetaInfo requestMetaInfo) {
        log.info("알림 조회 시작 - userId: {}", userId);
        List<Notification> notifications = notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(userId);

        List<NotificationResponseDTO> dtos = new ArrayList<>();

        for (Notification n : notifications) {
            String thumbnailUrl = null;

            log.info("댓글 알림 조회 시작 - userId: {}", userId);
            if (n.getType() == NotificationType.COMMENT && n.getRelatedId() != null) {
                    Long diaryId = Long.parseLong(n.getRelatedId());
                    Optional<Photo> representPhotoOpt = photoRepository.findFirstByDiaryIdAndRepresentIsTrue(diaryId);
                    thumbnailUrl = representPhotoOpt
                            .map(Photo::getUrl)
                            .map(url -> imagePathToUrlConverter.diaryImageUrl(url, requestMetaInfo))
                            .orElse(null);
            }
            else if (n.getType() == NotificationType.FRIEND && n.getRelatedId() != null) {
                log.info("친구 알림 조회 시작 - userId: {}", userId);
                User friend = userReader.getUserById(n.getRelatedId());
                    thumbnailUrl = imagePathToUrlConverter.userAvatarImageUrl(friend.getAvatar(), requestMetaInfo);
            }

            dtos.add(new NotificationResponseDTO(
                    n.getId(),
                    n.getRelatedId(),
                    n.getIsRead(),
                    n.getReceiverId(),
                    n.getMessage(),
                    thumbnailUrl
            ));
        }
        return dtos;
    }

    public Optional<Notification> findNotificationById(String notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            throw new NotificationNotFoundException("알림이 존재하지 않습니다. ID: " + notificationId);
        }
        return notificationOpt;
    }

    @Transactional
    public boolean markAsRead(Long notificationId, String userId) {

        Optional<Notification> notificationOpt = findNotificationById(String.valueOf(notificationId));

        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();

            if (!notification.getReceiverId().equals(userId)) {
                log.warn("사용자 {}가 본인의 알림이 아닌 알림 {}에 접근 시도", userId, notificationId);
                return false;
            }

            notification.markAsRead();
            return true;
        }

        return false;
    }

    @Transactional
    public boolean deleteNotification(Long notificationId, String userId) {
        Optional<Notification> notificationOpt = findNotificationById(String.valueOf(notificationId));

        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();

            if (!notification.getReceiverId().equals(userId)) {
                log.warn("사용자 {}가 본인의 알림이 아닌 알림 {}에 삭제 시도", userId, notificationId);
                return false;
            }
            notification.inactive();
            return true;
        }
        return false;
    }

}
