package com.pikume.back.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.notification.application.port.out.DeleteNotificationPort;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.SaveNotificationPort;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.exception.NotificationNotFoundException;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements LoadNotificationPort, SaveNotificationPort, DeleteNotificationPort {

	private final NotificationJpaRepository notificationJpaRepository;

	@Override
	public long countUnreadByReceiverId(String receiverId) {
		return notificationJpaRepository.countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(receiverId);
	}

	@Override
	public boolean existsFriendRequestByReceiverId(String receiverId) {
		return notificationJpaRepository.existsFriendRequestByReceiverId(receiverId);
	}

	@Override
	public Notification findById(Long notificationId) {
		return notificationJpaRepository.findById(notificationId)
				.orElseThrow(() -> new NotificationNotFoundException("알림이 존재하지 않습니다. ID: " + notificationId));
	}

	@Override
	public int markAllAsReadByReceiverId(String receiverId) {
		return notificationJpaRepository.markAllAsReadByReceiverId(receiverId);
	}

	@Override
	public Notification save(Notification notification) {
		return notificationJpaRepository.save(notification);
	}

	@Override
	public int deleteByDiaryId(Long diaryId) {
		return notificationJpaRepository.deleteByDiaryId(diaryId);
	}
}
