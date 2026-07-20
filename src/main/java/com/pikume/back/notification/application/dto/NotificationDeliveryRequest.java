package com.pikume.back.notification.application.dto;

public record NotificationDeliveryRequest(
		Long notificationId,
		String receiverId,
		NotificationStreamMessage streamMessage,
		String pushBody
) {
}
