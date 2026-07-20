package com.pikume.back.notification.adapter.out.persistence;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pikume.back.notification.domain.FcmToken;
import org.hibernate.NonUniqueResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenPersistenceAdapter")
class FcmTokenPersistenceAdapterTest {

	@InjectMocks
	private FcmTokenPersistenceAdapter adapter;

	@Mock
	private FcmTokenJpaRepository fcmTokenJpaRepository;

	@Test
	@DisplayName("사용자의 Push 전달 Token을 조회한다")
	void loadsPushDeliveryTokens() {
		given(fcmTokenJpaRepository.findAllByUserId("user-id"))
				.willReturn(List.of(
						new FcmToken("user-id", "token-1", "device-1"),
						new FcmToken("user-id", "token-2", "device-2")));

		assertThat(adapter.loadPushDeliveryTokens("user-id"))
				.containsExactlyInAnyOrder("token-1", "token-2");
	}

	@Test
	@DisplayName("기존 기기의 Push Token을 교체한다")
	void updatesExistingDeviceToken() {
		FcmToken existing = new FcmToken("user-id", "old-token", "device-1");
		given(fcmTokenJpaRepository.findByUserIdAndDeviceId("user-id", "device-1"))
				.willReturn(Optional.of(existing));

		adapter.registerPushToken("user-id", "new-token", "device-1");

		assertThat(existing.getToken()).isEqualTo("new-token");
		then(fcmTokenJpaRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("새 기기의 Push Token을 저장한다")
	void storesNewDeviceToken() {
		given(fcmTokenJpaRepository.findByUserIdAndDeviceId("user-id", "device-1"))
				.willReturn(Optional.empty());

		adapter.registerPushToken("user-id", "token-1", "device-1");

		then(fcmTokenJpaRepository).should().save(any(FcmToken.class));
	}

	@Test
	@DisplayName("전송 실패 Token과 기기별 Token을 각각 해제한다")
	void revokesPushTokens() {
		adapter.revokePushToken("bad-token");
		adapter.revokePushTokenForDevice("user-id", "device-1");

		then(fcmTokenJpaRepository).should().deleteByToken("bad-token");
		then(fcmTokenJpaRepository).should().deleteByUserIdAndDeviceId("user-id", "device-1");
	}

	@Test
	@DisplayName("중복 Token과 기기별 해제 로그에 raw deviceId를 남기지 않는다")
	void doesNotLogRawDeviceId() {
		given(fcmTokenJpaRepository.findByUserIdAndDeviceId("user-id", "sensitive-device-id"))
				.willThrow(new NonUniqueResultException(2));
		ListAppender<ILoggingEvent> appender = attachLogAppender();

		try {
			adapter.registerPushToken("user-id", "token-123", "sensitive-device-id");
			adapter.revokePushTokenForDevice("user-id", "sensitive-device-id");
		} finally {
			detachLogAppender(appender);
		}

		assertThat(appender.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.noneMatch(message -> message.contains("sensitive-device-id"));
	}

	private ListAppender<ILoggingEvent> attachLogAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(FcmTokenPersistenceAdapter.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(FcmTokenPersistenceAdapter.class);
		logger.detachAppender(appender);
	}
}
