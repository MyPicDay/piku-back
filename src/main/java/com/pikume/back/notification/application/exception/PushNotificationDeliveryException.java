package com.pikume.back.notification.application.exception;

public class PushNotificationDeliveryException extends RuntimeException {

	public PushNotificationDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}
}
