package com.pikume.back.notification.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.adapter.out.persistence.PhotoJpaRepository;
import com.pikume.back.diary.adapter.out.storage.PhotoConstants;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.notification.adapter.out.persistence.NotificationJpaRepository;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceQueryIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationJpaRepository notificationJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private PhotoJpaRepository photoJpaRepository;

	@Test
	@DisplayName("알림 목록 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void notificationQueryCountStaysBounded() {
		User receiver = saveUser("receiver");
		User sender1 = saveUser("sender1");
		User sender2 = saveUser("sender2");
		User sender3 = saveUser("sender3");

		Diary diary1 = saveDiary(sender1.getId(), "notification-1");
		Diary diary2 = saveDiary(sender2.getId(), "notification-2");
		Diary diary3 = saveDiary(sender3.getId(), "notification-3");

		saveRepresentPhoto(diary1, "notification-1.jpg");
		saveRepresentPhoto(diary2, "notification-2.jpg");
		saveRepresentPhoto(diary3, "notification-3.jpg");

		notificationJpaRepository.save(new Notification(receiver.getId(), sender1.getId(), NotificationType.COMMENT, diary1.getId()));
		notificationJpaRepository.save(new Notification(receiver.getId(), sender2.getId(), NotificationType.COMMENT, diary2.getId()));
		notificationJpaRepository.save(new Notification(receiver.getId(), sender3.getId(), NotificationType.COMMENT, diary3.getId()));

		long oneItemQueries = measurePreparedStatements(() ->
				notificationService.getNotifications(receiver.getId(), REQUEST_META_INFO, PageRequest.of(0, 1)));
		long threeItemQueries = measurePreparedStatements(() ->
				notificationService.getNotifications(receiver.getId(), REQUEST_META_INFO, PageRequest.of(0, 3)));

		assertThat(threeItemQueries)
				.as("알림 row 수가 늘어도 발신자/썸네일 조회를 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	private User saveUser(String suffix) {
		return userJpaRepository.save(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + suffix,
				"avatars/" + suffix + ".png"));
	}

	private Diary saveDiary(String userId, String content) {
		return diaryJpaRepository.save(new Diary(content, DiaryVisibility.PUBLIC, LocalDate.now(), userId));
	}

	private void saveRepresentPhoto(Diary diary, String fileName) {
		Photo photo = new Photo(diary, PhotoConstants.PUBLIC_PREFIX + diary.getUserId() + "/" + fileName, 0);
		photo.updateRepresent(true);
		photoJpaRepository.save(photo);
	}
}
