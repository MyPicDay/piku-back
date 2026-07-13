package com.pikume.back.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
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
		Map<Long, NotificationDiaryInfo> diariesById = loadDiaryInfos(notifications.getContent());
		List<Notification> visibleNotifications = notifications.getContent().stream()
				.filter(notification -> isVisibleNotification(notification, diariesById))
				.toList();
		Map<String, UserSummaryView> usersById = loadUsers(visibleNotifications, diariesById);

		List<NotificationListView> content = visibleNotifications.stream()
				.map(notification -> toNotificationListView(notification, usersById, diariesById))
				.toList();
		long filteredCount = notifications.getContent().size() - visibleNotifications.size();
		return new PageResult<>(
				content,
				notifications.getNumber(),
				notifications.getSize(),
				Math.max(0, notifications.getTotalElements() - filteredCount));
	}

	private boolean isVisibleNotification(Notification notification, Map<Long, NotificationDiaryInfo> diariesById) {
		return notification.getDiaryId() == null || diariesById.containsKey(notification.getDiaryId());
	}

	private Map<String, UserSummaryView> loadUsers(List<Notification> notifications, Map<Long, NotificationDiaryInfo> diariesById) {
		Set<String> senderIds = notifications.stream()
				.filter(notification -> !isAnonymousDiaryNotification(notification, diariesById))
				.map(Notification::getSenderId)
				.filter(senderId -> senderId != null && !senderId.isBlank())
				.collect(Collectors.toSet());
		return queryUserSummaryUseCase.queryUserSummaries(senderIds);
	}

	private boolean isAnonymousDiaryNotification(Notification notification, Map<Long, NotificationDiaryInfo> diariesById) {
		if (notification.getDiaryId() == null) {
			return false;
		}
		NotificationDiaryInfo diaryInfo = diariesById.get(notification.getDiaryId());
		return diaryInfo != null && diaryInfo.anonymous();
	}

	private Map<Long, NotificationDiaryInfo> loadDiaryInfos(List<Notification> notifications) {
		Set<Long> diaryIds = notifications.stream()
				.map(Notification::getDiaryId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
		if (diaryIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, DiarySummaryView> summariesById = queryDiaryReadUseCase.getDiarySummaries(diaryIds);
		Map<Long, String> thumbnailsByDiaryId = queryDiaryReadUseCase.getRepresentPhotoPaths(diaryIds).entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> resolveImageUrlPort.getPhotoUrl(entry.getValue(), true)));

		return summariesById.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> {
							Long diaryId = entry.getKey();
							DiarySummaryView diary = entry.getValue();
							return new NotificationDiaryInfo(
									thumbnailsByDiaryId.get(diaryId),
									diary.userId(),
									diary.status() == DiaryVisibility.ANONYMOUS);
						}));
	}

	private NotificationListView toNotificationListView(Notification notification, Map<String, UserSummaryView> usersById,
			Map<Long, NotificationDiaryInfo> diariesById) {
		UserSummaryView sender = notification.getSenderId() != null ? usersById.get(notification.getSenderId()) : null;
		NotificationDiaryInfo diaryInfo = notification.getDiaryId() != null ? diariesById.get(notification.getDiaryId()) : null;

		return new NotificationListView(
				notification.getId(),
				sender != null ? sender.nickname() : null,
				sender != null ? sender.avatarPath() : null,
				notification.getType(),
				notification.getDiaryId(),
				diaryInfo != null ? diaryInfo.thumbnailUrl() : null,
				notification.getIsRead(),
				notification.getCreatedAt(),
				null,
				diaryInfo != null ? diaryInfo.diaryUserId() : null,
				diaryInfo != null && diaryInfo.anonymous());
	}

	private record NotificationDiaryInfo(
			String thumbnailUrl,
			String diaryUserId,
			boolean anonymous
	) {
	}
}
