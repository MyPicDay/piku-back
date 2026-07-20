package com.pikume.back.notification.application.stream;

import com.pikume.back.notification.application.dto.NotificationStreamMessage;

import java.util.function.Consumer;

public interface NotificationStreamConnection {

	void onCompletion(Runnable action);

	void onTimeout(Runnable action);

	void onError(Consumer<Throwable> action);

	void complete();

	void send(NotificationStreamMessage message);
}
