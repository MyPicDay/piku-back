package com.pikume.back.notification.application.port.out;

public interface MarkNotificationReadPort {

	void markNotificationReadIfActive(Long notificationId, String receiverId);
}
