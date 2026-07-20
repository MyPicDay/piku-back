package com.pikume.back.notification.application.readmodel;

public record NotificationSummaryView(
		long unreadCount,
		boolean hasFriendRequest
) {
}
