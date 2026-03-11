package com.pikume.back.notification.adapter.in.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.port.in.NotificationUseCase;
import com.pikume.back.notification.domain.vo.NotificationType;
import com.pikume.back.social.domain.event.SocialEvent;

/**
 * Social 도메인 이벤트를 수신하여 알림을 발송하는 리스너.
 * Notification 모듈에 위치하여 Social → Notification 방향의 의존을 이벤트 기반으로 처리합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialEventListener {

	private final NotificationUseCase notificationUseCase;

	@EventListener
	public void handleCommentCreated(SocialEvent.CommentCreatedEvent event) {
		log.info("[SocialEventListener] 댓글 생성 이벤트 수신 - receiverId: {}, senderId: {}, diaryId: {}",
				event.receiverId(), event.senderId(), event.diaryId());

		NotificationType type = event.isReply() ? NotificationType.REPLY : NotificationType.COMMENT;
		notificationUseCase.sendNotification(
				event.receiverId(), type, event.senderId(), event.diaryId(), null);
	}

	@EventListener
	public void handleLikeCreated(SocialEvent.LikeCreatedEvent event) {
		log.info("[SocialEventListener] 좋아요 이벤트 수신 - receiverId: {}, senderId: {}, diaryId: {}",
				event.receiverId(), event.senderId(), event.diaryId());

		notificationUseCase.sendNotification(
				event.receiverId(), NotificationType.LIKE,
				event.senderId(), event.diaryId(), null);
	}

	@EventListener
	public void handleFriendRequest(SocialEvent.FriendRequestEvent event) {
		log.info("[SocialEventListener] 친구 요청 이벤트 수신 - receiverId: {}, senderId: {}",
				event.receiverId(), event.senderId());

		notificationUseCase.sendNotification(
				event.receiverId(), NotificationType.FRIEND_REQUEST,
				event.senderId(), null, null);
	}

	@EventListener
	public void handleFriendAccepted(SocialEvent.FriendAcceptedEvent event) {
		log.info("[SocialEventListener] 친구 수락 이벤트 수신 - receiverId: {}, senderId: {}",
				event.receiverId(), event.senderId());

		notificationUseCase.sendNotification(
				event.receiverId(), NotificationType.FRIEND_ACCEPT,
				event.senderId(), null, null);
	}
}
