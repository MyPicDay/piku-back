package com.pikume.back.notification.application.dto;

public record NotificationStreamMessage(String eventId, String eventName, Object data) {
}
