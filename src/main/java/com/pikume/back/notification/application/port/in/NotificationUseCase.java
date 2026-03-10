package com.pikume.back.notification.application.port.in;

import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.domain.vo.NotificationType;

public interface NotificationUseCase {

	void sendNotification(String receiverId, NotificationType type, String senderId,
			Long diaryId, RequestMetaInfo requestMetaInfo);

	PageResult<NotificationResult> getNotifications(String receiverId, RequestMetaInfo requestMetaInfo,
			PageQuery pageQuery);

	boolean markAsRead(Long notificationId, String userId);

	void markAllAsRead(String userId);

	boolean deleteNotification(Long notificationId, String userId);
}
