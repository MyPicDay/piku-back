package com.pikume.back.notification.adapter.out.persistence;

import com.pikume.back.notification.application.service.NotificationDeletionService;
import com.pikume.back.notification.application.service.NotificationReadService;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("NotificationJpaRepository soft delete")
class NotificationJpaRepositorySoftDeleteTest {

	@Autowired
	private NotificationJpaRepository notificationJpaRepository;

	@Test
	@DisplayName("삭제된 친구 요청 알림만 존재하면 활성 친구 요청이 아니다")
	void excludesDeletedFriendRequestFromActiveSummary() {
		Notification notification = notificationJpaRepository.save(
				new Notification("receiver-id", "sender-id", NotificationType.FRIEND_REQUEST, null));
		notification.delete();
		notificationJpaRepository.saveAndFlush(notification);

		boolean exists = notificationJpaRepository.existsActiveFriendRequestByReceiverId("receiver-id");

		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("삭제된 알림은 활성 단건 조회에서 제외한다")
	void excludesDeletedNotificationFromActiveLookup() {
		Notification notification = deletedNotification();

		assertThat(notificationJpaRepository.findByIdAndDeletedAtIsNull(notification.getId())).isEmpty();
	}

	@Test
	@DisplayName("삭제된 알림은 읽음 처리하지 않는다")
	void doesNotMarkDeletedNotificationRead() {
		Notification notification = deletedNotification();
		NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(notificationJpaRepository);
		NotificationReadService service = new NotificationReadService(adapter, adapter);

		boolean result = service.markNotificationRead(notification.getId(), "receiver-id");

		assertThat(result).isFalse();
		assertThat(notificationJpaRepository.findById(notification.getId()).orElseThrow().getIsRead()).isFalse();
	}

	@Test
	@DisplayName("존재하지 않는 알림은 읽음 처리하지 않는다")
	void doesNotMarkMissingNotificationRead() {
		NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(notificationJpaRepository);
		NotificationReadService service = new NotificationReadService(adapter, adapter);

		boolean result = service.markNotificationRead(Long.MAX_VALUE, "receiver-id");

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("삭제된 알림은 다시 삭제하지 않는다")
	void doesNotDeleteDeletedNotificationAgain() {
		Notification notification = deletedNotification();
		LocalDateTime firstDeletedAt = notification.getDeletedAt();
		NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(notificationJpaRepository);
		NotificationDeletionService service = new NotificationDeletionService(adapter, adapter);

		boolean result = service.deleteNotification(notification.getId(), "receiver-id");

		assertThat(result).isFalse();
		assertThat(notificationJpaRepository.findById(notification.getId()).orElseThrow().getDeletedAt())
				.isEqualTo(firstDeletedAt);
	}

	@Test
	@DisplayName("존재하지 않는 알림은 삭제하지 않는다")
	void doesNotDeleteMissingNotification() {
		NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(notificationJpaRepository);
		NotificationDeletionService service = new NotificationDeletionService(adapter, adapter);

		boolean result = service.deleteNotification(Long.MAX_VALUE, "receiver-id");

		assertThat(result).isFalse();
	}

	private Notification deletedNotification() {
		Notification notification = notificationJpaRepository.save(
				new Notification("receiver-id", "sender-id", NotificationType.COMMENT, 10L));
		notification.delete();
		return notificationJpaRepository.saveAndFlush(notification);
	}
}
