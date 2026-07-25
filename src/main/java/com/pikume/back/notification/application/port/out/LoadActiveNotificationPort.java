package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.domain.Notification;

import java.util.Optional;

public interface LoadActiveNotificationPort {

	Optional<Notification> loadActiveNotification(Long notificationId);
}
