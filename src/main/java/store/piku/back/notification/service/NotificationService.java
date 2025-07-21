package store.piku.back.notification.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.notification.dto.NotificationDTO;
import store.piku.back.notification.dto.response.CommentNotificationDTO;
import store.piku.back.notification.entity.Notification;
import store.piku.back.notification.entity.NotificationType;
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
public class NotificationService {

    private static final Long DEFAULT_TIMEOUT = 60L*1000*60;

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final UserReader userReader;
    private final PhotoRepository photoRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;


    // SSE 연결 ( 알림 구독 시작할 때)
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
    public void saveNotification(String receiverId, NotificationType type, String message, String relatedId) {

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


    private void sendLostData(String lastEventId, String userId, String emitterId, SseEmitter emitter) {
        Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByUserId(String.valueOf(userId));
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
    }


    public List<CommentNotificationDTO> getNotifications(String userId, RequestMetaInfo requestMetaInfo) {
        List<Notification> notifications = notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(userId);

        List<CommentNotificationDTO> dtos = new ArrayList<>();

        for (Notification n : notifications) {
            String thumbnailUrl = null;

            if (n.getType() == NotificationType.COMMENT && n.getRelatedId() != null) {
                try {
                    Long diaryId = Long.parseLong(n.getRelatedId());
                    Optional<Photo> representPhotoOpt = photoRepository.findFirstByDiaryIdAndRepresentIsTrue(diaryId);
                    thumbnailUrl = representPhotoOpt
                            .map(Photo::getUrl)
                            .map(url -> imagePathToUrlConverter.diaryImageUrl(url, requestMetaInfo))
                            .orElse(null);
                } catch (NumberFormatException e) {
                    // relatedId가 숫자가 아닐 경우 예외 처리
                    // 필요하면 로그 남기기
                }
            }
            else if (n.getType() == NotificationType.FRIEND && n.getRelatedId() != null) {
                try {
                    User friend = userReader.getUserById(n.getRelatedId());
                    thumbnailUrl = imagePathToUrlConverter.userAvatarImageUrl(friend.getAvatar(), requestMetaInfo);
                } catch (Exception e) {
                    // 유저 조회 실패 시 처리
                }
            }

            dtos.add(new CommentNotificationDTO(
                    n.getRelatedId(),
                    n.getIsRead(),
                    n.getReceiverId(),
                    n.getMessage(),
                    thumbnailUrl
            ));
        }
        return dtos;
    }


    @Transactional
    public boolean markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(String.valueOf(notificationId));
        if (notificationOpt.isPresent()) {
            notificationOpt.get().markAsRead();
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteNotification(Long notificationId,String userId) {
        Notification notification = notificationRepository.findById(String.valueOf(notificationId)).orElse(null);

        if (notificationRepository.existsById(String.valueOf(notificationId))&& notification.getReceiverId().equals(userId)) {
            notificationRepository.deleteById(String.valueOf(notificationId));
            return true;
        }
        return false;
    }

}
