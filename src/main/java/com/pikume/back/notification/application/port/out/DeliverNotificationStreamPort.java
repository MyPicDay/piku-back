package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.dto.NotificationStreamMessage;

public interface DeliverNotificationStreamPort {

	void deliverNotificationStream(String userId, NotificationStreamMessage message);
}
