package com.pikume.back.notification.application.port.out;

public interface DeliverPushNotificationPort {

	void deliverPushNotification(String targetToken, String body);
}
