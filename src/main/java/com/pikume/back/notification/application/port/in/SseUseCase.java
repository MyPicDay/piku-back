package com.pikume.back.notification.application.port.in;

import com.pikume.back.notification.application.port.out.NotificationStreamConnection;

public interface SseUseCase {

	void subscribe(String userId, NotificationStreamConnection connection);
}
