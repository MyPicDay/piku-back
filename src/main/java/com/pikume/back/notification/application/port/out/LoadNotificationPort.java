package com.pikume.back.notification.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.notification.domain.Notification;

public interface LoadNotificationPort {

	long countUnreadByReceiverId(String receiverId);

	boolean existsFriendRequestByReceiverId(String receiverId);

	Page<Notification> findAllByReceiverIdAndDeletedAtIsNull(String receiverId, Pageable pageable);

	Notification findById(Long notificationId);

	int markAllAsReadByReceiverId(String receiverId);
}
