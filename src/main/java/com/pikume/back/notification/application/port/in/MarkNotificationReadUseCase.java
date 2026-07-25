package com.pikume.back.notification.application.port.in;

public interface MarkNotificationReadUseCase {

	void markNotificationRead(Long notificationId, String userId);
}
