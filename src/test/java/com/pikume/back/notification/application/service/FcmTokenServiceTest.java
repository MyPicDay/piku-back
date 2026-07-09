package com.pikume.back.notification.application.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import com.pikume.back.notification.application.port.out.PushNotificationPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService - FCM 토큰 관리")
class FcmTokenServiceTest {

	@InjectMocks
	private FcmTokenService fcmTokenService;

	@Mock
	private PushNotificationPort pushNotificationPort;

	@Nested
	@DisplayName("saveToken - FCM 토큰 저장")
	class SaveToken {

		@Test
		@DisplayName("FCM 토큰 저장을 PushNotificationPort에 위임한다")
		void delegatesToPushPort() {
			fcmTokenService.saveToken("user-id", "token-123", "device-1");

			then(pushNotificationPort).should().saveToken("user-id", "token-123", "device-1");
		}

		@Test
		@DisplayName("서로 다른 디바이스의 토큰을 각각 저장한다")
		void savesTokenForDifferentDevices() {
			fcmTokenService.saveToken("user-id", "token-1", "device-a");
			fcmTokenService.saveToken("user-id", "token-2", "device-b");

			then(pushNotificationPort).should().saveToken("user-id", "token-1", "device-a");
			then(pushNotificationPort).should().saveToken("user-id", "token-2", "device-b");
		}

		@Test
		@DisplayName("FCM 토큰 저장 로그에 raw deviceId를 남기지 않는다")
		void saveTokenDoesNotLogRawDeviceId() {
			ListAppender<ILoggingEvent> appender = attachLogAppender();

			try {
				fcmTokenService.saveToken("user-id", "token-123", "sensitive-device-id");
			} finally {
				detachLogAppender(appender);
			}

			assertThat(formattedMessages(appender))
					.noneMatch(message -> message.contains("sensitive-device-id"));
		}
	}

	@Nested
	@DisplayName("revokeTokenForDevice - FCM 토큰 해제")
	class RevokeTokenForDevice {

		@Test
		@DisplayName("userId와 deviceId 조합의 FCM 토큰 해제를 PushNotificationPort에 위임한다")
		void delegatesDeviceTokenRevocationToPushPort() {
			fcmTokenService.revokeTokenForDevice("user-id", "device-1");

			then(pushNotificationPort).should().deleteTokenForDevice("user-id", "device-1");
		}

		@Test
		@DisplayName("FCM 토큰 해제 로그에 raw deviceId를 남기지 않는다")
		void revokeTokenForDeviceDoesNotLogRawDeviceId() {
			ListAppender<ILoggingEvent> appender = attachLogAppender();

			try {
				fcmTokenService.revokeTokenForDevice("user-id", "sensitive-device-id");
			} finally {
				detachLogAppender(appender);
			}

			assertThat(formattedMessages(appender))
					.noneMatch(message -> message.contains("sensitive-device-id"));
		}
	}

	private ListAppender<ILoggingEvent> attachLogAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(FcmTokenService.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(FcmTokenService.class);
		logger.detachAppender(appender);
	}

	private java.util.List<String> formattedMessages(ListAppender<ILoggingEvent> appender) {
		return appender.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.toList();
	}
}
