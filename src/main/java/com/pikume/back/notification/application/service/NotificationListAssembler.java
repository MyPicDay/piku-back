package com.pikume.back.notification.application.service;

import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.policy.NotificationPresentationPolicy;
import com.pikume.back.notification.application.port.out.LoadNotificationDiaryContextsPort;
import com.pikume.back.notification.application.port.out.LoadNotificationSendersPort;
import com.pikume.back.notification.application.readmodel.NotificationDiaryContextView;
import com.pikume.back.notification.application.readmodel.NotificationListItemView;
import com.pikume.back.notification.application.readmodel.NotificationSenderView;
import com.pikume.back.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationListAssembler {

	private final LoadNotificationSendersPort loadNotificationSendersPort;
	private final LoadNotificationDiaryContextsPort loadNotificationDiaryContextsPort;
	private final NotificationPresentationPolicy presentationPolicy;

	public PageResult<NotificationListItemView> assemble(PageResult<Notification> notifications) {
		Map<Long, NotificationDiaryContextView> diaryContexts = loadDiaryContexts(notifications.getContent());
		List<Notification> visibleNotifications = notifications.getContent().stream()
				.filter(notification -> notification.getDiaryId() == null
						|| diaryContexts.containsKey(notification.getDiaryId()))
				.toList();
		Map<String, NotificationSenderView> senders = loadSenders(visibleNotifications, diaryContexts);
		List<NotificationListItemView> content = visibleNotifications.stream()
				.map(notification -> toListItem(notification, senders, diaryContexts))
				.toList();
		long filteredCount = notifications.getContent().size() - visibleNotifications.size();
		return new PageResult<>(
				content,
				notifications.getNumber(),
				notifications.getSize(),
				Math.max(0, notifications.getTotalElements() - filteredCount));
	}

	private Map<Long, NotificationDiaryContextView> loadDiaryContexts(List<Notification> notifications) {
		Set<Long> diaryIds = notifications.stream()
				.map(Notification::getDiaryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		return diaryIds.isEmpty()
				? Map.of()
				: loadNotificationDiaryContextsPort.loadNotificationDiaryContexts(diaryIds);
	}

	private Map<String, NotificationSenderView> loadSenders(
			List<Notification> notifications,
			Map<Long, NotificationDiaryContextView> diaryContexts) {
		Set<String> senderIds = notifications.stream()
				.filter(notification -> !isAnonymous(notification, diaryContexts))
				.map(Notification::getSenderId)
				.filter(senderId -> senderId != null && !senderId.isBlank())
				.collect(Collectors.toSet());
		return senderIds.isEmpty()
				? Map.of()
				: loadNotificationSendersPort.loadNotificationSenders(senderIds);
	}

	private NotificationListItemView toListItem(
			Notification notification,
			Map<String, NotificationSenderView> senders,
			Map<Long, NotificationDiaryContextView> diaryContexts) {
		NotificationDiaryContextView diaryContext = notification.getDiaryId() == null
				? null
				: diaryContexts.get(notification.getDiaryId());
		boolean anonymous = diaryContext != null && diaryContext.anonymous();
		NotificationSenderView sender = anonymous ? null : senders.get(notification.getSenderId());
		NotificationKind kind = NotificationKind.valueOf(notification.getType().name());
		return new NotificationListItemView(
				notification.getId(),
				presentationPolicy.messageFor(kind),
				presentationPolicy.senderNickname(anonymous, sender != null ? sender.nickname() : null),
				sender != null ? sender.avatarUrl() : null,
				kind,
				notification.getDiaryId(),
				diaryContext != null ? diaryContext.thumbnailUrl() : null,
				notification.getIsRead(),
				notification.getCreatedAt(),
				diaryContext != null ? diaryContext.diaryDate() : null,
				anonymous || diaryContext == null ? null : diaryContext.diaryUserId());
	}

	private boolean isAnonymous(
			Notification notification,
			Map<Long, NotificationDiaryContextView> diaryContexts) {
		if (notification.getDiaryId() == null) {
			return false;
		}
		NotificationDiaryContextView diaryContext = diaryContexts.get(notification.getDiaryId());
		return diaryContext != null && diaryContext.anonymous();
	}
}
