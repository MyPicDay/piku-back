package com.pikume.back.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.notification.application.port.out.DeleteNotificationsByDiaryPort;
import com.pikume.back.notification.application.port.out.LoadNotificationPagePort;
import com.pikume.back.notification.application.port.out.LoadNotificationPort;
import com.pikume.back.notification.application.port.out.LoadNotificationSummaryPort;
import com.pikume.back.notification.application.port.out.MarkAllNotificationsReadPort;
import com.pikume.back.notification.application.port.out.MarkNotificationReadPort;
import com.pikume.back.notification.application.port.out.RecordNotificationPort;
import com.pikume.back.notification.application.readmodel.NotificationSummaryView;
import com.pikume.back.notification.domain.Notification;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements
		LoadNotificationPort,
		LoadNotificationPagePort,
		LoadNotificationSummaryPort,
		RecordNotificationPort,
		MarkNotificationReadPort,
		MarkAllNotificationsReadPort,
		DeleteNotificationsByDiaryPort {

	private final NotificationJpaRepository notificationJpaRepository;

	@Override
	public Optional<Notification> loadActiveNotification(Long notificationId) {
		return notificationJpaRepository.findByIdAndDeletedAtIsNull(notificationId);
	}

	@Override
	public PageResult<Notification> loadNotificationPage(String receiverId, PageQuery pageQuery) {
		return SpringPageMapper.toPageResult(
				notificationJpaRepository.findAllByReceiverIdAndDeletedAtIsNull(
						receiverId,
						SpringPageMapper.toPageable(pageQuery)));
	}

	@Override
	public NotificationSummaryView loadNotificationSummary(String receiverId) {
		return new NotificationSummaryView(
				notificationJpaRepository.countByReceiverIdAndIsReadFalseAndDeletedAtIsNull(receiverId),
				notificationJpaRepository.existsActiveFriendRequestByReceiverId(receiverId));
	}

	@Override
	public Notification recordNotification(Notification notification) {
		return notificationJpaRepository.save(notification);
	}

	@Override
	public void markNotificationReadIfActive(Long notificationId, String receiverId) {
		notificationJpaRepository.markAsReadIfActive(notificationId, receiverId);
	}

	@Override
	public int markAllNotificationsRead(String receiverId) {
		return notificationJpaRepository.markAllAsReadByReceiverId(receiverId);
	}

	@Override
	public int deleteNotificationsByDiary(Long diaryId) {
		return notificationJpaRepository.deleteByDiaryId(diaryId);
	}
}
