package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.port.in.DeleteNotificationUseCase;
import com.pikume.back.notification.application.port.in.DeleteNotificationsByDiaryUseCase;
import com.pikume.back.notification.application.port.out.DeleteNotificationsByDiaryPort;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeletionService implements DeleteNotificationUseCase, DeleteNotificationsByDiaryUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final DeleteNotificationsByDiaryPort deleteNotificationsByDiaryPort;

	@Override
	@Transactional
	public boolean deleteNotification(Long notificationId, String userId) {
		var notificationResult = loadNotificationPort.loadActiveNotification(notificationId);
		if (notificationResult.isEmpty()) {
			log.warn("event=notification_delete_denied outcome=not_found userId={} notificationId={}",
					userId, notificationId);
			return false;
		}
		Notification notification = notificationResult.get();
		if (!notification.getReceiverId().equals(userId)) {
			log.warn("event=notification_delete_denied outcome=denied userId={} notificationId={}",
					userId, notificationId);
			return false;
		}
		notification.delete();
		return true;
	}

	@Override
	@Transactional
	public void deleteNotificationsByDiary(Long diaryId) {
		if (diaryId == null) {
			return;
		}
		int deletedCount = deleteNotificationsByDiaryPort.deleteNotificationsByDiary(diaryId);
		log.info("event=diary_notifications_deleted outcome=success diaryId={} count={}", diaryId, deletedCount);
	}
}
