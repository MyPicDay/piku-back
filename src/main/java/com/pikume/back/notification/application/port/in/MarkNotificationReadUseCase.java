package com.pikume.back.notification.application.port.in;

public interface MarkNotificationReadUseCase {

	boolean markNotificationRead(Long notificationId, String userId);
}
