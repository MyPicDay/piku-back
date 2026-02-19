package store.piku.back.notification.application.port.out;

import store.piku.back.notification.domain.Notification;

public interface SaveNotificationPort {

	Notification save(Notification notification);
}
