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
	@DisplayName("비운영 환경에서는 Push 전달 요청만 기록한다")
	void logsLocalPushDelivery() {
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			localPushAdapter.deliverPushNotification("target-token", "알림 본문");
		} finally {
			detachLogAppender(appender);
		}

		assertThat(formattedMessages(appender))
				.anyMatch(message -> message.contains("알림 본문"))
				.noneMatch(message -> message.contains("target-token"));
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
