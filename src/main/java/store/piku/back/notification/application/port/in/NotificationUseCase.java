package store.piku.back.notification.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.notification.adapter.in.web.dto.NotificationResponseDTO;
import store.piku.back.notification.domain.vo.NotificationType;

public interface NotificationUseCase {

	void sendNotification(String receiverId, NotificationType type, String senderId,
			Long diaryId, RequestMetaInfo requestMetaInfo);

	Page<NotificationResponseDTO> getNotifications(String receiverId, RequestMetaInfo requestMetaInfo,
			Pageable pageable);

	boolean markAsRead(Long notificationId, String userId);

	void markAllAsRead(String userId);

	boolean deleteNotification(Long notificationId, String userId);
}
