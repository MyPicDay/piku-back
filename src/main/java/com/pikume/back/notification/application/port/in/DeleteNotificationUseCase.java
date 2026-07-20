package com.pikume.back.notification.application.port.in;

public interface DeleteNotificationUseCase {

	boolean deleteNotification(Long notificationId, String userId);
}
