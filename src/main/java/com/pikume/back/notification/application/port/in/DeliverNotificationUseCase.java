package com.pikume.back.notification.application.port.in;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;

public interface DeliverNotificationUseCase {

	void deliverNotification(NotificationDeliveryRequest request);
}
