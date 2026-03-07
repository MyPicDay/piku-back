package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.domain.Notification;

public interface LoadNotificationPort {

	long countUnreadByReceiverId(String receiverId);

	boolean existsFriendRequestByReceiverId(String receiverId);

	Notification findById(Long notificationId);

	int markAllAsReadByReceiverId(String receiverId);
}
