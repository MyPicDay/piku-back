package store.piku.back.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.diary.service.PhotoStorageService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.notification.dto.response.NotificationResponseDTO;
import store.piku.back.notification.dto.response.SseResponse;
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
    private final NotificationProvider notificationProvider;
    private final PhotoStorageService photoStorageService;


    public SseEmitter subscribe(String userId) {

        log.info("[Emitter 생성 요청]");
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> {
            log.info("[Emitter 종료 - Completion] emitterId: {}", emitterId);
            emitterRepository.deleteById(emitterId);
        });

        emitter.onTimeout(() -> {
            log.warn("[Emitter 종료 - Timeout] emitterId: {}", emitterId);
            emitterRepository.deleteById(emitterId);
        });

        log.info("[안읽은 알림 개수 조회 요청] userId: {}", userId);
        long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(userId);

        String eventId = userId + "_" + System.currentTimeMillis();

        log.info("[클라이언트로 초기 데이터 전송 요청]");
        sendToClient(emitter, eventId, emitterId, unreadCount);

        return emitter;
    }


    public void sendToClient(SseEmitter emitter, String eventId, String emitterId,SseResponse response) {
        try {
            log.info("[이벤트 전송 시도] eventId: {}, message: {}", eventId, response);
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .data(response));
        } catch (IOException e) {
            log.info("클라이언트와 연결 끊김, emitter 삭제 요청");
            emitterRepository.deleteById(emitterId);
            throw new RuntimeException("연결 오류!");
        }
    }
    public void sendToClient(SseEmitter emitter, String eventId, String emitterId, Long count) {
        try {
            log.info("[이벤트 초기 전송 시도]");
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .data(count));
        } catch (IOException e) {
            log.info("(초기) 클라이언트와 연결 끊김, emitter 삭제 요청");
            emitterRepository.deleteById(emitterId);
            throw new RuntimeException("연결 오류!");
        }
    }


    @Transactional
    public void sendNotification(String receiverId, NotificationType type, String senderId, Diary diary, RequestMetaInfo requestMetaInfo ) {

        log.info("알림 저장 요청 ");
        User sender = userReader.getUserById(senderId);
        Notification notification = new Notification(receiverId, sender, type, diary);
        notificationRepository.save(notification);

        String eventId = receiverId + "_" + System.currentTimeMillis();
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(receiverId);
        String message = generateMessage(type);


        String senderNickname = sender.getNickname();
        String senderAvatarUrl = imagePathToUrlConverter.userAvatarImageUrl(sender.getAvatar(), requestMetaInfo);


        String thumbnailUrl = null;
        if (diary != null) {
            Optional<Photo> representPhotoOpt = photoRepository.findFirstByDiaryIdAndRepresentIsTrue(diary.getId());
            thumbnailUrl = representPhotoOpt
                    .map(Photo::getUrl)
                    .map(photoStorageService::getPhotoUrl)
                    .orElse(null);
        }

        SseResponse notificationDTO = new SseResponse(
                type,
                message,
                diary != null ? diary.getId() : null,
                senderId,
                senderNickname,
                senderAvatarUrl,
                thumbnailUrl
        );


        emitters.forEach((emitterId, emitter) -> {
            try {
                log.info("[SSE 알림 전송] receiverId: {}, emitterId: {}", receiverId, emitterId);
                sendToClient(emitter, eventId, emitterId, notificationDTO);
            } catch (Exception e) {
                log.warn("SSE 알림 전송 실패: {}", e.getMessage());
            }
        });

        try {
            String token = notificationProvider.getTokenByUserId(receiverId);
            if(token != null) {
                notificationProvider.sendMessage(token, message);
            }
        } catch (Exception e) {
            log.warn("FCM 전송 실패: {}", e.getMessage());
        }
    }


    public String generateMessage(NotificationType type) {

        return switch (type) {
            case FRIEND_REQUEST -> "님이 댓글을 달았습니다.";
            case FRIEND_ACCEPT -> "님이 친구 요청을 보냈습니다.";
            case COMMENT -> "님이 일기에 댓글을 달았습니다.";
            case REPLY -> "님이 회원님의 댓글에 답글들 달았습니다.";
            case FRIEND_DIARY -> "님이 새 일기를 작성하였습니다.";
        };
    }

    @Transactional
    public List<NotificationResponseDTO> getNotifications(String receiverId, RequestMetaInfo requestMetaInfo) {
        log.info("알림 조회 시작 - 받는사람 | receiverId: {}", receiverId);
        List<Notification> notifications = notificationRepository.findAllByReceiverIdAndDeletedAtIsNullOrderByCreatedAtDesc(receiverId);

        List<NotificationResponseDTO> dtos = new ArrayList<>();

        for (Notification n : notifications) {
            User sender = n.getSender();
            String senderNickname = sender.getNickname();
            String senderAvatarUrl = imagePathToUrlConverter.userAvatarImageUrl(sender.getAvatar(), requestMetaInfo);

            NotificationType type = n.getType();
            String message = generateMessage(n.getType());

            Long relatedDiaryId = null;
            String thumbnailUrl = null;
            if (n.getRelatedDiary() != null) {
                Diary diary = n.getRelatedDiary();
                relatedDiaryId = diary.getId();

                Optional<Photo> representPhotoOpt = photoRepository.findFirstByDiaryIdAndRepresentIsTrue(relatedDiaryId);
                thumbnailUrl = representPhotoOpt
                        .map(Photo::getUrl)
                        .map(photoStorageService::getPhotoUrl)
                        .orElse(null);
            }

            dtos.add(new NotificationResponseDTO(
                    n.getId(),
                    message,
                    senderNickname,
                    senderAvatarUrl,
                    type,
                    relatedDiaryId,
                    thumbnailUrl,
                    n.getIsRead(),
                    n.getCreatedAt()
            ));
        }
        return dtos;
    }

    public Optional<Notification> findNotificationById(String notificationId) {

        log.info("[알림 조회 요청] notificationId: {}", notificationId);

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
