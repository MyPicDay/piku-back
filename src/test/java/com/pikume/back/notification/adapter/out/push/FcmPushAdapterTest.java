package com.pikume.back.notification.adapter.out.push;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pikume.back.notification.adapter.out.persistence.FcmTokenJpaRepository;
import org.hibernate.NonUniqueResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmPushAdapter")
class FcmPushAdapterTest {

	@InjectMocks
	private FcmPushAdapter fcmPushAdapter;

	@Mock
	private FcmTokenJpaRepository fcmTokenJpaRepository;

	@Test
	@DisplayName("기기별 FCM 토큰 해제 시 userId와 deviceId 조합으로 삭제한다")
	void deleteTokenForDeviceDeletesByUserIdAndDeviceId() {
		fcmPushAdapter.deleteTokenForDevice("user-id", "device-1");

		then(fcmTokenJpaRepository).should().deleteByUserIdAndDeviceId("user-id", "device-1");
	}

	@Test
	@DisplayName("기기별 FCM 토큰 해제 로그에 raw deviceId를 남기지 않는다")
	void deleteTokenForDeviceDoesNotLogRawDeviceId() {
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			fcmPushAdapter.deleteTokenForDevice("user-id", "sensitive-device-id");
		} finally {
			detachLogAppender(appender);
		}

		assertThat(formattedMessages(appender))
				.noneMatch(message -> message.contains("sensitive-device-id"));
	}

	@Test
	@DisplayName("중복 FCM 토큰 오류 로그에 raw deviceId를 남기지 않는다")
	void saveTokenDuplicateErrorDoesNotLogRawDeviceId() {
		given(fcmTokenJpaRepository.findByUserIdAndDeviceId("user-id", "sensitive-device-id"))
				.willThrow(new NonUniqueResultException(2));
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			fcmPushAdapter.saveToken("user-id", "token-123", "sensitive-device-id");
		} finally {
			detachLogAppender(appender);
		}

		assertThat(formattedMessages(appender))
				.noneMatch(message -> message.contains("sensitive-device-id"));
	}

	private ListAppender<ILoggingEvent> attachLogAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(FcmPushAdapter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(FcmPushAdapter.class);
		logger.detachAppender(appender);
	}

	private java.util.List<String> formattedMessages(ListAppender<ILoggingEvent> appender) {
		return appender.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.toList();
	}
}
