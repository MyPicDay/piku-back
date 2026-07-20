package com.pikume.back.notification.application.port.out;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.domain.Notification;

public interface LoadNotificationPagePort {

	PageResult<Notification> loadNotificationPage(String receiverId, PageQuery pageQuery);
}
