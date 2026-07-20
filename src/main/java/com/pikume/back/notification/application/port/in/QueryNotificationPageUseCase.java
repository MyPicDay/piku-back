package com.pikume.back.notification.application.port.in;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationResult;

public interface QueryNotificationPageUseCase {

	PageResult<NotificationResult> queryNotifications(String receiverId, PageQuery pageQuery);
}
