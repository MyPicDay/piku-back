package com.pikume.back.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.adapter.out.persistence.PhotoJpaRepository;
import com.pikume.back.diary.adapter.out.storage.MinioPhotoStorageAdapter;
import com.pikume.back.notification.application.port.out.LoadNotificationListViewPort;
import com.pikume.back.notification.application.readmodel.NotificationListView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationListViewPersistenceAdapter implements LoadNotificationListViewPort {

	private final NotificationJpaRepository notificationJpaRepository;
	private final UserJpaRepository userJpaRepository;
	private final PhotoJpaRepository photoJpaRepository;
	private final MinioPhotoStorageAdapter minioPhotoStorageAdapter;

	@Override
	public Page<NotificationListView> loadNotifications(String receiverId, Pageable pageable) {
		Page<Notification> notifications = notificationJpaRepository.findAllByReceiverIdAndDeletedAtIsNull(receiverId, pageable);
		Map<String, User> usersById = loadUsers(notifications.getContent());
		Map<Long, String> thumbnailsByDiaryId = loadThumbnails(notifications.getContent());

		List<NotificationListView> content = notifications.getContent().stream()
				.map(notification -> toNotificationListView(notification, usersById, thumbnailsByDiaryId))
				.toList();
		return new PageImpl<>(content, pageable, notifications.getTotalElements());
	}

	private Map<String, User> loadUsers(List<Notification> notifications) {
		Set<String> senderIds = notifications.stream()
				.map(Notification::getSenderId)
				.filter(senderId -> senderId != null && !senderId.isBlank())
				.collect(Collectors.toSet());
		if (senderIds.isEmpty()) {
			return Map.of();
		}

		return userJpaRepository.findAllById(senderIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
	}

	private Map<Long, String> loadThumbnails(List<Notification> notifications) {
		Set<Long> diaryIds = notifications.stream()
				.map(Notification::getDiaryId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
		if (diaryIds.isEmpty()) {
			return Map.of();
		}

		return photoJpaRepository.findRepresentPhotoUrlsByDiaryIds(diaryIds).stream()
				.collect(Collectors.toMap(
						PhotoJpaRepository.DiaryThumbnailProjection::getDiaryId,
						projection -> minioPhotoStorageAdapter.getPhotoUrl(projection.getUrl(), true)));
	}

	private NotificationListView toNotificationListView(Notification notification, Map<String, User> usersById,
			Map<Long, String> thumbnailsByDiaryId) {
		User sender = notification.getSenderId() != null ? usersById.get(notification.getSenderId()) : null;

		return new NotificationListView(
				notification.getId(),
				sender != null ? sender.getNickname() : null,
				sender != null ? sender.getAvatar() : null,
				notification.getType(),
				notification.getDiaryId(),
				notification.getDiaryId() != null ? thumbnailsByDiaryId.get(notification.getDiaryId()) : null,
				notification.getIsRead(),
				notification.getCreatedAt(),
				null,
				null);
	}
}
