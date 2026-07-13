package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.dto.NotificationStreamMessage;

public interface NotificationStreamPort {

	void save(String emitterId, String userId, NotificationStreamConnection connection);

	void sendToUser(String userId, NotificationStreamMessage message);

	void delete(String userId, String emitterId);
}
