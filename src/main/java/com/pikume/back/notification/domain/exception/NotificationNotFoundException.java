package com.pikume.back.notification.domain.exception;

public class NotificationNotFoundException extends RuntimeException {
	public NotificationNotFoundException(String message) {
		super(message);
	}
}
