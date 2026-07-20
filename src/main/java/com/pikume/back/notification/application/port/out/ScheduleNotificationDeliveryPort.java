package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;

public interface ScheduleNotificationDeliveryPort {

	void scheduleNotificationDelivery(NotificationDeliveryRequest request);
}
