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
import com.pikume.back.notification.application.port.out.RecordNotificationPort;
import com.pikume.back.notification.application.readmodel.NotificationSummaryView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.application.exception.NotificationNotFoundException;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements
		LoadNotificationPort,
		LoadNotificationPagePort,
		LoadNotificationSummaryPort,
		RecordNotificationPort,
		MarkAllNotificationsReadPort,
		DeleteNotificationsByDiaryPort {

	private final NotificationJpaRepository notificationJpaRepository;

	@Override
	public Notification loadNotification(Long notificationId) {
		return findById(notificationId);
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
				notificationJpaRepository.existsFriendRequestByReceiverId(receiverId));
	}

	@Override
	public Notification recordNotification(Notification notification) {
		return notificationJpaRepository.save(notification);
	}

	@Override
	public int markAllNotificationsRead(String receiverId) {
		return notificationJpaRepository.markAllAsReadByReceiverId(receiverId);
	}

	@Override
	public int deleteNotificationsByDiary(Long diaryId) {
		return notificationJpaRepository.deleteByDiaryId(diaryId);
	}

	private Notification findById(Long notificationId) {
		return notificationJpaRepository.findById(notificationId)
				.orElseThrow(() -> new NotificationNotFoundException("알림이 존재하지 않습니다. ID: " + notificationId));
	}
}
