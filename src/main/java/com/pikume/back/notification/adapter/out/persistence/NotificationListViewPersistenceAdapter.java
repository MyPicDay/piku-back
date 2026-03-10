package com.pikume.back.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.notification.application.port.out.LoadNotificationListViewPort;
import com.pikume.back.notification.application.readmodel.NotificationListView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationListViewPersistenceAdapter implements LoadNotificationListViewPort {

	private final NotificationJpaRepository notificationJpaRepository;
	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final ResolveImageUrlPort resolveImageUrlPort;

	@Override
	public PageResult<NotificationListView> loadNotifications(String receiverId, PageQuery pageQuery) {
		org.springframework.data.domain.Page<Notification> notifications =
				notificationJpaRepository.findAllByReceiverIdAndDeletedAtIsNull(receiverId, SpringPageMapper.toPageable(pageQuery));
		Map<String, UserSummaryView> usersById = loadUsers(notifications.getContent());
		Map<Long, String> thumbnailsByDiaryId = loadThumbnails(notifications.getContent());

		List<NotificationListView> content = notifications.getContent().stream()
				.map(notification -> toNotificationListView(notification, usersById, thumbnailsByDiaryId))
				.toList();
		return new PageResult<>(content, notifications.getNumber(), notifications.getSize(), notifications.getTotalElements());
	}

	private Map<String, UserSummaryView> loadUsers(List<Notification> notifications) {
		Set<String> senderIds = notifications.stream()
				.map(Notification::getSenderId)
				.filter(senderId -> senderId != null && !senderId.isBlank())
				.collect(Collectors.toSet());
		return queryUserSummaryUseCase.getUserSummaries(senderIds);
	}

	private Map<Long, String> loadThumbnails(List<Notification> notifications) {
		Set<Long> diaryIds = notifications.stream()
				.map(Notification::getDiaryId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
		if (diaryIds.isEmpty()) {
			return Map.of();
		}

		return queryDiaryReadUseCase.getRepresentPhotoPaths(diaryIds).entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> resolveImageUrlPort.getPhotoUrl(entry.getValue(), true)));
	}

	private NotificationListView toNotificationListView(Notification notification, Map<String, UserSummaryView> usersById,
			Map<Long, String> thumbnailsByDiaryId) {
		UserSummaryView sender = notification.getSenderId() != null ? usersById.get(notification.getSenderId()) : null;

		return new NotificationListView(
				notification.getId(),
				sender != null ? sender.nickname() : null,
				sender != null ? sender.avatarPath() : null,
				notification.getType(),
				notification.getDiaryId(),
				notification.getDiaryId() != null ? thumbnailsByDiaryId.get(notification.getDiaryId()) : null,
				notification.getIsRead(),
				notification.getCreatedAt(),
				null,
				null);
	}
}
