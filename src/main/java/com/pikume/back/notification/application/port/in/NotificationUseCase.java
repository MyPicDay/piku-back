package com.pikume.back.notification.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.notification.adapter.in.web.dto.NotificationResponseDTO;
import com.pikume.back.notification.domain.vo.NotificationType;

public interface NotificationUseCase {

	void sendNotification(String receiverId, NotificationType type, String senderId,
			Long diaryId, RequestMetaInfo requestMetaInfo);

	Page<NotificationResponseDTO> getNotifications(String receiverId, RequestMetaInfo requestMetaInfo,
			Pageable pageable);

	boolean markAsRead(Long notificationId, String userId);

	void markAllAsRead(String userId);

	boolean deleteNotification(Long notificationId, String userId);
}
