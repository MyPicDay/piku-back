package com.pikume.back.notification.application.service;

import com.pikume.back.notification.application.port.in.MarkAllNotificationsReadUseCase;
import com.pikume.back.notification.application.port.in.MarkNotificationReadUseCase;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.MarkAllNotificationsReadPort;
import com.pikume.back.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationReadService implements MarkNotificationReadUseCase, MarkAllNotificationsReadUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final MarkAllNotificationsReadPort markAllNotificationsReadPort;

	@Override
	@Transactional
	public boolean markNotificationRead(Long notificationId, String userId) {
		Notification notification = loadNotificationPort.loadNotification(notificationId);
		if (!notification.getReceiverId().equals(userId)) {
			log.warn("event=notification_read_denied outcome=denied userId={} notificationId={}",
					userId, notificationId);
			return false;
		}
		notification.markAsRead();
		return true;
	}

	@Override
	@Transactional
	public void markAllNotificationsRead(String userId) {
		int updatedCount = markAllNotificationsReadPort.markAllNotificationsRead(userId);
		log.info("event=notifications_read outcome=success userId={} count={}", userId, updatedCount);
	}
}
