package com.pikume.back.notification.adapter.out.push;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalPushAdapter")
class LocalPushAdapterTest {

	private final LocalPushAdapter localPushAdapter = new LocalPushAdapter();

	@Test
	@DisplayName("기기별 FCM 토큰 해제 로그에 raw deviceId를 남기지 않는다")
	void deleteTokenForDeviceDoesNotLogRawDeviceId() {
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			localPushAdapter.deleteTokenForDevice("user-id", "sensitive-device-id");
		} finally {
			detachLogAppender(appender);
		}

		assertThat(formattedMessages(appender))
				.noneMatch(message -> message.contains("sensitive-device-id"));
	}

	private ListAppender<ILoggingEvent> attachLogAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(LocalPushAdapter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(LocalPushAdapter.class);
		logger.detachAppender(appender);
	}

	private java.util.List<String> formattedMessages(ListAppender<ILoggingEvent> appender) {
		return appender.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.toList();
	}
}
