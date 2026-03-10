package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.dto.NotificationStreamMessage;

public interface NotificationStreamConnection {

	void onCompletion(Runnable action);

	void onTimeout(Runnable action);

	void complete();

	void send(NotificationStreamMessage message);
}
