package com.pikume.back.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.dto.NotificationSsePayload;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.port.in.NotificationUseCase;
import com.pikume.back.notification.application.port.out.*;
import com.pikume.back.notification.application.readmodel.NotificationListView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final LoadUserForNotificationPort loadUserForNotificationPort;
	private final LoadDiaryForNotificationPort loadDiaryForNotificationPort;
	private final LoadNotificationListViewPort loadNotificationListViewPort;
	private final PushNotificationPort pushNotificationPort;
	private final NotificationStreamPort notificationStreamPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	@Override
	@Transactional
	public void sendNotification(String receiverId, NotificationType type, String senderId,
			Long diaryId, RequestMetaInfo requestMetaInfo) {
		log.info("알림 저장 요청");
		Notification notification = new Notification(receiverId, senderId, type, diaryId);
		saveNotificationPort.save(notification);

		String eventId = receiverId + "_" + System.currentTimeMillis();
		String message = generateMessage(type);

		String senderNickname = loadUserForNotificationPort.getUserNickname(senderId);
		String senderAvatar = loadUserForNotificationPort.getUserAvatar(senderId);
		String senderAvatarUrl = loadUserForNotificationPort.getUserAvatarUrl(senderAvatar, requestMetaInfo);

		String thumbnailUrl = null;
		if (diaryId != null) {
			thumbnailUrl = loadDiaryForNotificationPort.getDiaryThumbnailUrl(diaryId);
		}

		NotificationSsePayload notificationDTO = new NotificationSsePayload(
				type, message, diaryId, senderId,
				senderNickname, senderAvatarUrl, thumbnailUrl);

		String eventName = (type == NotificationType.FRIEND_REQUEST) ? "FriendRequest" : null;
		notificationStreamPort.sendToUser(receiverId, new NotificationStreamMessage(eventId, eventName, notificationDTO));

		sendPushNotification(receiverId, senderNickname + message);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<NotificationResult> getNotifications(String receiverId, RequestMetaInfo requestMetaInfo,
			PageQuery pageQuery) {
		log.info("알림 조회 시작 - receiverId: {}", receiverId);
		PageResult<NotificationListView> notifications = loadNotificationListViewPort.loadNotifications(receiverId, pageQuery);
		return notifications.map(notification -> toNotificationResponse(notification, requestMetaInfo));
	}

	@Override
	@Transactional
	public boolean markAsRead(Long notificationId, String userId) {
		Notification notification = loadNotificationPort.findById(notificationId);

		if (!notification.getReceiverId().equals(userId)) {
			log.warn("사용자 {}가 본인의 알림이 아닌 알림 {}에 접근 시도", userId, notificationId);
			return false;
		}

		notification.markAsRead();
		return true;
	}

	@Override
	@Transactional
	public void markAllAsRead(String userId) {
		log.info("알림 모두 읽음 처리 시작 - userId: {}", userId);
		int updatedCount = loadNotificationPort.markAllAsReadByReceiverId(userId);
		log.info("알림 모두 읽음 처리 완료 - userId: {}, 업데이트된 알림 수: {}", userId, updatedCount);
	}

	@Override
	@Transactional
	public boolean deleteNotification(Long notificationId, String userId) {
		Notification notification = loadNotificationPort.findById(notificationId);

		if (!notification.getReceiverId().equals(userId)) {
			log.warn("사용자 {}가 본인의 알림이 아닌 알림 {}에 삭제 시도", userId, notificationId);
			return false;
		}

		notification.inactive();
		return true;
	}

	// ─── Private Helpers ───

	String generateMessage(NotificationType type) {
		return switch (type) {
			case FRIEND_REQUEST -> "님이 친구 요청을 보냈습니다";
			case FRIEND_ACCEPT -> "님이 친구 수락했습니다.";
			case COMMENT -> "님이 일기에 댓글을 달았습니다.";
			case REPLY -> "님이 회원님의 댓글에 답글을 달았습니다.";
			case FRIEND_DIARY -> "님이 새 일기를 작성하였습니다.";
			case LIKE -> "님이 회원님의 일기를 좋아합니다.";
		};
	}

	private void sendPushNotification(String receiverId, String body) {
		try {
			Set<String> tokens = pushNotificationPort.getTokenByUserId(receiverId);
			if (tokens.isEmpty()) {
				log.warn("FCM 토큰이 없습니다. receiverId: {}", receiverId);
				return;
			}
			for (String token : tokens) {
				try {
					log.info("[FCM 알림 전송] receiverId: {}, token: {}", receiverId, token);
					pushNotificationPort.sendMessage(token, body);
				} catch (Exception e) {
					log.debug("FCM 알림 전송 실패: {}", e.getMessage());
					pushNotificationPort.deleteToken(token);
				}
			}
		} catch (Exception e) {
			log.warn("FCM 전송 실패: {}", e.getMessage());
		}
	}

	private NotificationResult toNotificationResponse(NotificationListView notification, RequestMetaInfo requestMetaInfo) {
		String senderAvatarUrl = notification.senderAvatarPath() != null
				? imagePathToUrlConverter.userAvatarImageUrl(notification.senderAvatarPath(), requestMetaInfo)
				: null;

		return new NotificationResult(
				notification.notificationId(),
				generateMessage(notification.type()),
				notification.senderNickname(),
				senderAvatarUrl,
				notification.type(),
				notification.diaryId(),
				notification.thumbnailUrl(),
				notification.isRead(),
				notification.createdAt(),
				notification.diaryDate(),
				notification.diaryUserId());
	}
}
