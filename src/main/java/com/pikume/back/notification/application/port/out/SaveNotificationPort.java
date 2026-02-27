package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.domain.Notification;

public interface SaveNotificationPort {

	Notification save(Notification notification);
}
