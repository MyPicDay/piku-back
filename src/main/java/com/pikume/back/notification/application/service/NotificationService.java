package com.pikume.back.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

	private static final String ANONYMOUS_NICKNAME = "익명";

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final DeleteNotificationPort deleteNotificationPort;
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
		LoadDiaryForNotificationPort.DiaryNotificationInfo diaryInfo = loadDiaryNotificationInfo(diaryId);
		if (diaryId != null && diaryInfo == null) {
			log.info("알림 생성 생략 - 일기를 찾을 수 없습니다. diaryId: {}", diaryId);
			return;
		}
		Notification notification = new Notification(receiverId, senderId, type, diaryId);
		saveNotificationPort.save(notification);

		String eventId = receiverId + "_" + System.currentTimeMillis();
		String message = generateMessage(type);

		boolean anonymousDiary = diaryInfo != null && diaryInfo.anonymous();
		String senderNickname = ANONYMOUS_NICKNAME;
		String senderAvatarUrl = null;
		String responseSenderId = null;

		if (!anonymousDiary) {
			senderNickname = loadUserForNotificationPort.getUserNickname(senderId);
			String senderAvatar = loadUserForNotificationPort.getUserAvatar(senderId);
			senderAvatarUrl = loadUserForNotificationPort.getUserAvatarUrl(senderAvatar, requestMetaInfo);
			responseSenderId = senderId;
		}

		NotificationSsePayload notificationDTO = new NotificationSsePayload(
				type, message, diaryId, responseSenderId,
				senderNickname, senderAvatarUrl, diaryInfo != null ? diaryInfo.thumbnailUrl() : null);

		String eventName = (type == NotificationType.FRIEND_REQUEST) ? "FriendRequest" : null;
		NotificationStreamMessage streamMessage = new NotificationStreamMessage(eventId, eventName, notificationDTO);
		String pushBody = senderNickname + message;

		sendAfterCommit(receiverId, streamMessage, pushBody);
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

		notification.delete();
		return true;
	}

	@Override
	@Transactional
	public void deleteNotificationsByDiaryId(Long diaryId) {
		if (diaryId == null) {
			return;
		}
		int deletedCount = deleteNotificationPort.deleteByDiaryId(diaryId);
		log.info("일기 {} 관련 알림 삭제 완료 - count: {}", diaryId, deletedCount);
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

	private void sendAfterCommit(String receiverId, NotificationStreamMessage streamMessage, String pushBody) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			sendNotificationMessage(receiverId, streamMessage, pushBody);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sendNotificationMessage(receiverId, streamMessage, pushBody);
			}
		});
	}

	private void sendNotificationMessage(String receiverId, NotificationStreamMessage streamMessage, String pushBody) {
		sendStreamNotification(receiverId, streamMessage);
		sendPushNotification(receiverId, pushBody);
	}

	private void sendStreamNotification(String receiverId, NotificationStreamMessage streamMessage) {
		try {
			notificationStreamPort.sendToUser(receiverId, streamMessage);
		} catch (RuntimeException e) {
			log.warn("SSE 알림 전송 실패: {}", e.getMessage());
		}
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
		String senderAvatarUrl = !notification.anonymousDiary() && notification.senderAvatarPath() != null
				? imagePathToUrlConverter.userAvatarImageUrl(notification.senderAvatarPath(), requestMetaInfo)
				: null;

		return new NotificationResult(
				notification.notificationId(),
				generateMessage(notification.type()),
				notification.anonymousDiary() ? ANONYMOUS_NICKNAME : notification.senderNickname(),
				senderAvatarUrl,
				notification.type(),
				notification.diaryId(),
				notification.thumbnailUrl(),
				notification.isRead(),
				notification.createdAt(),
				notification.diaryDate(),
				notification.anonymousDiary() ? null : notification.diaryUserId());
	}

	private LoadDiaryForNotificationPort.DiaryNotificationInfo loadDiaryNotificationInfo(Long diaryId) {
		if (diaryId == null) {
			return null;
		}
		LoadDiaryForNotificationPort.DiaryNotificationInfo diaryInfo =
				loadDiaryForNotificationPort.getDiaryNotificationInfo(diaryId);
		if (diaryInfo != null) {
			return diaryInfo;
		}
		return null;
	}
}
