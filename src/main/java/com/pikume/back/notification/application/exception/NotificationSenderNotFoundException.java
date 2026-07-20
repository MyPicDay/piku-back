package com.pikume.back.notification.application.exception;

public class NotificationSenderNotFoundException extends RuntimeException {

	public NotificationSenderNotFoundException(String senderId) {
		super("알림 발신자 정보를 찾을 수 없습니다. senderId: " + senderId);
	}
}
