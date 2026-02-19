package store.piku.back.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import store.piku.back.notification.application.port.out.LoadNotificationPort;
import store.piku.back.notification.application.port.out.SaveNotificationPort;
import store.piku.back.notification.domain.Notification;
import store.piku.back.notification.domain.exception.NotificationNotFoundException;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements LoadNotificationPort, SaveNotificationPort {

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
	public Page<Notification> findAllByReceiverIdAndDeletedAtIsNull(String receiverId, Pageable pageable) {
		return notificationJpaRepository.findAllByReceiverIdAndDeletedAtIsNull(receiverId, pageable);
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
}
