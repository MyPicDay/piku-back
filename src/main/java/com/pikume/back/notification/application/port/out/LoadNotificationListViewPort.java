package com.pikume.back.notification.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.readmodel.NotificationListView;

public interface LoadNotificationListViewPort {

	PageResult<NotificationListView> loadNotifications(String receiverId, PageQuery pageQuery);
}
