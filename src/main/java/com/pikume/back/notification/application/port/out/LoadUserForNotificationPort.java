package com.pikume.back.notification.application.port.out;

public interface LoadUserForNotificationPort {

	String getUserNickname(String userId);

	String getUserAvatar(String userId);

	String getUserAvatarUrl(String avatar);
}
