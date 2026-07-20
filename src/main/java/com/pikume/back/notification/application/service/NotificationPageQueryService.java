package com.pikume.back.notification.application.service;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.port.in.QueryNotificationPageUseCase;
import com.pikume.back.notification.application.port.out.LoadNotificationPagePort;
import com.pikume.back.notification.application.readmodel.NotificationListItemView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPageQueryService implements QueryNotificationPageUseCase {

	private final LoadNotificationPagePort loadNotificationPagePort;
	private final NotificationListAssembler notificationListAssembler;

	@Override
	@Transactional(readOnly = true)
	public PageResult<NotificationResult> queryNotifications(String receiverId, PageQuery pageQuery) {
		return notificationListAssembler
				.assemble(loadNotificationPagePort.loadNotificationPage(receiverId, pageQuery))
				.map(this::toResult);
	}

	private NotificationResult toResult(NotificationListItemView item) {
		return new NotificationResult(
				item.id(),
				item.message(),
				item.nickname(),
				item.avatarUrl(),
				item.kind(),
				item.relatedDiaryId(),
				item.thumbnailUrl(),
				item.isRead(),
				item.createdAt(),
				item.diaryDate(),
				item.diaryUserId());
	}
}
