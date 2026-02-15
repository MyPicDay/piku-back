package store.piku.back.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.service.NotificationService;
import store.piku.back.social.domain.event.SocialEvent;

/**
 * Social 도메인 이벤트를 수신하여 알림을 발송하는 리스너.
 * Notification 모듈에 위치하여 Social → Notification 방향의 의존을 이벤트 기반으로 처리합니다.
 *
 * Note: RequestMetaInfo가 이벤트에 포함되지 않으므로 null을 전달합니다.
 * SSE/FCM 알림에서 avatar URL 생성 시 영향이 있을 수 있으나, 기존 동작을 유지합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialEventListener {

	private final NotificationService notificationService;
	private final DiaryRepository diaryRepository;

	@EventListener
	public void handleCommentCreated(SocialEvent.CommentCreatedEvent event) {
		log.info("[SocialEventListener] 댓글 생성 이벤트 수신 - receiverId: {}, senderId: {}, diaryId: {}",
				event.getReceiverId(), event.getSenderId(), event.getDiaryId());

		Diary diary = diaryRepository.findById(event.getDiaryId()).orElse(null);
		NotificationType type = event.isReply() ? NotificationType.REPLY : NotificationType.COMMENT;

		notificationService.sendNotification(
				event.getReceiverId(),
				type,
				event.getSenderId(),
				diary,
				null);
	}

	@EventListener
	public void handleLikeCreated(SocialEvent.LikeCreatedEvent event) {
		log.info("[SocialEventListener] 좋아요 이벤트 수신 - receiverId: {}, senderId: {}, diaryId: {}",
				event.getReceiverId(), event.getSenderId(), event.getDiaryId());

		Diary diary = diaryRepository.findById(event.getDiaryId()).orElse(null);

		notificationService.sendNotification(
				event.getReceiverId(),
				NotificationType.LIKE,
				event.getSenderId(),
				diary,
				null);
	}

	@EventListener
	public void handleFriendRequest(SocialEvent.FriendRequestEvent event) {
		log.info("[SocialEventListener] 친구 요청 이벤트 수신 - receiverId: {}, senderId: {}",
				event.getReceiverId(), event.getSenderId());

		notificationService.sendNotification(
				event.getReceiverId(),
				NotificationType.FRIEND_REQUEST,
				event.getSenderId(),
				null,
				null);
	}

	@EventListener
	public void handleFriendAccepted(SocialEvent.FriendAcceptedEvent event) {
		log.info("[SocialEventListener] 친구 수락 이벤트 수신 - receiverId: {}, senderId: {}",
				event.getReceiverId(), event.getSenderId());

		notificationService.sendNotification(
				event.getReceiverId(),
				NotificationType.FRIEND_ACCEPT,
				event.getSenderId(),
				null,
				null);
	}
}
