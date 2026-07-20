package com.pikume.back.notification.application.port.in;

import com.pikume.back.notification.application.stream.NotificationStreamConnection;

public interface SubscribeNotificationStreamUseCase {

	void subscribeToNotifications(String userId, NotificationStreamConnection connection);
}
