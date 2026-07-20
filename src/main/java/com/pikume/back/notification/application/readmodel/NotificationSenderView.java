package com.pikume.back.notification.application.readmodel;

public record NotificationSenderView(
		String userId,
		String nickname,
		String avatarUrl
) {
}
