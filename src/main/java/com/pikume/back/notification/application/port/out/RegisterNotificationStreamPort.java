package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.stream.NotificationStreamConnection;

public interface RegisterNotificationStreamPort {

	void registerNotificationStream(String connectionId, String userId, NotificationStreamConnection connection);
}
