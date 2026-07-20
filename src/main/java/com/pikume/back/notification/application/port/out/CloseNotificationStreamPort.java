package com.pikume.back.notification.application.port.out;

public interface CloseNotificationStreamPort {

	void closeNotificationStream(String userId, String connectionId);
}
