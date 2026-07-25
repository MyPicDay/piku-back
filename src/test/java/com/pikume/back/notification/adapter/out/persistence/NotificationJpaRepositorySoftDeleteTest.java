package com.pikume.back.notification.adapter.out.persistence;

import com.pikume.back.notification.application.service.NotificationDeletionService;
import com.pikume.back.notification.application.service.NotificationReadService;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@DisplayName("NotificationJpaRepository soft delete")
class NotificationJpaRepositorySoftDeleteTest {

	@Autowired
	private NotificationJpaRepository notificationJpaRepository;
	@Autowired
	private EntityManager entityManager;

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
	@DisplayName("소유자의 활성 미확인 알림을 읽음 처리한다")
	void marksOwnedActiveUnreadNotificationRead() {
		Notification notification = notificationJpaRepository.saveAndFlush(
				new Notification("receiver-id", "sender-id", NotificationType.COMMENT, 10L));
		LocalDateTime previousUpdatedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
		setUpdatedAt(notification.getId(), previousUpdatedAt);

		int updated = notificationJpaRepository.markAsReadIfActive(notification.getId(), "receiver-id");

		Notification result = notificationJpaRepository.findById(notification.getId()).orElseThrow();
		assertThat(updated).isEqualTo(1);
		assertThat(result.getIsRead()).isTrue();
		assertThat(result.getUpdatedAt()).isAfter(previousUpdatedAt);
	}

	@Test
	@DisplayName("레거시 null 읽음 상태도 미확인 알림으로 읽음 처리한다")
	void marksLegacyNullReadStateNotificationRead() {
		Notification notification = notificationJpaRepository.saveAndFlush(
				new Notification(
						null,
						"receiver-id",
						"sender-id",
						NotificationType.COMMENT,
						10L,
						null,
						null));

		int updated = notificationJpaRepository.markAsReadIfActive(notification.getId(), "receiver-id");

		assertThat(updated).isEqualTo(1);
		assertThat(notificationJpaRepository.findById(notification.getId()).orElseThrow().getIsRead()).isTrue();
	}

	@Test
	@DisplayName("다른 수신자의 알림은 읽음 처리하지 않는다")
	void doesNotMarkAnotherReceiversNotificationRead() {
		Notification notification = notificationJpaRepository.saveAndFlush(
				new Notification("receiver-id", "sender-id", NotificationType.COMMENT, 10L));
		LocalDateTime previousUpdatedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
		setUpdatedAt(notification.getId(), previousUpdatedAt);

		int updated = notificationJpaRepository.markAsReadIfActive(notification.getId(), "other-id");

		Notification result = notificationJpaRepository.findById(notification.getId()).orElseThrow();
		assertThat(updated).isZero();
		assertThat(result.getIsRead()).isFalse();
		assertThat(result.getUpdatedAt()).isEqualTo(previousUpdatedAt);
	}

	@Test
	@DisplayName("이미 읽은 알림은 다시 갱신하지 않는다")
	void doesNotUpdateAlreadyReadNotification() {
		Notification notification = notificationJpaRepository.saveAndFlush(
				new Notification("receiver-id", "sender-id", NotificationType.COMMENT, 10L));
		notificationJpaRepository.markAsReadIfActive(notification.getId(), "receiver-id");
		LocalDateTime previousUpdatedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
		setUpdatedAt(notification.getId(), previousUpdatedAt);

		int updated = notificationJpaRepository.markAsReadIfActive(notification.getId(), "receiver-id");

		Notification result = notificationJpaRepository.findById(notification.getId()).orElseThrow();
		assertThat(updated).isZero();
		assertThat(result.getIsRead()).isTrue();
		assertThat(result.getUpdatedAt()).isEqualTo(previousUpdatedAt);
	}

	@Test
	@DisplayName("삭제된 알림은 읽음 처리하지 않는다")
	void doesNotMarkDeletedNotificationRead() {
		Notification notification = deletedNotification();
		LocalDateTime firstDeletedAt = notification.getDeletedAt();
		LocalDateTime previousUpdatedAt = notification.getUpdatedAt();
		NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(notificationJpaRepository);
		NotificationReadService service = new NotificationReadService(adapter, adapter);

		service.markNotificationRead(notification.getId(), "receiver-id");

		Notification result = notificationJpaRepository.findById(notification.getId()).orElseThrow();
		assertThat(result.getIsRead()).isFalse();
		assertThat(result.getDeletedAt()).isEqualTo(firstDeletedAt);
		assertThat(result.getUpdatedAt()).isEqualTo(previousUpdatedAt);
	}

	@Test
	@DisplayName("존재하지 않는 알림은 읽음 처리하지 않는다")
	void doesNotMarkMissingNotificationRead() {
		NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(notificationJpaRepository);
		NotificationReadService service = new NotificationReadService(adapter, adapter);

		assertThatCode(() -> service.markNotificationRead(Long.MAX_VALUE, "receiver-id"))
				.doesNotThrowAnyException();
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

	private void setUpdatedAt(Long notificationId, LocalDateTime updatedAt) {
		entityManager.createQuery("""
						UPDATE Notification n
						SET n.updatedAt = :updatedAt
						WHERE n.id = :notificationId
						""")
				.setParameter("updatedAt", updatedAt)
				.setParameter("notificationId", notificationId)
				.executeUpdate();
		entityManager.clear();
	}
}
