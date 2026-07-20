package com.pikume.back.notification.application.exception;

public class NotificationNotFoundException extends RuntimeException {
	public NotificationNotFoundException(String message) {
		super(message);
	}
}
