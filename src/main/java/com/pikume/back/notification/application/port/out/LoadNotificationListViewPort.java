package com.pikume.back.notification.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.notification.application.readmodel.NotificationListView;

public interface LoadNotificationListViewPort {

	Page<NotificationListView> loadNotifications(String receiverId, Pageable pageable);
}
