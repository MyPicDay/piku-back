package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.domain.Notification;

public interface LoadNotificationPort {

	Notification loadNotification(Long notificationId);
}
