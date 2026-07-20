package com.pikume.back.notification.adapter.out.persistence;

import com.pikume.back.notification.adapter.out.transaction.SpringNotificationDeliverySchedulerAdapter;
import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.service.NotificationDeliveryService;
import com.pikume.back.notification.domain.FcmToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(FcmTokenPersistenceAdapterIntegrationTest.Configuration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("FcmTokenPersistenceAdapter 영속성")
class FcmTokenPersistenceAdapterIntegrationTest {

	@Autowired
	private FcmTokenPersistenceAdapter adapter;

	@Autowired
	private FcmTokenJpaRepository fcmTokenJpaRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void clearTokens() {
		fcmTokenJpaRepository.deleteAll();
	}

	@Test
	@DisplayName("기존 기기의 Push Token 교체를 커밋한다")
	void commitsExistingDeviceTokenReplacement() {
		FcmToken existing = fcmTokenJpaRepository.saveAndFlush(
				new FcmToken("user-id", "old-token", "device-1"));

		adapter.registerPushToken("user-id", "new-token", "device-1");

		assertThat(fcmTokenJpaRepository.findById(existing.getId()))
				.map(FcmToken::getToken)
				.contains("new-token");
	}

	@Test
	@DisplayName("전송에 실패한 Push Token 해제를 커밋한다")
	void commitsFailedPushTokenRevocation() {
		FcmToken existing = fcmTokenJpaRepository.saveAndFlush(
				new FcmToken("user-id", "bad-token", "device-1"));

		adapter.revokePushToken("bad-token");

		assertThat(fcmTokenJpaRepository.findById(existing.getId())).isEmpty();
	}

	@Test
	@DisplayName("알림 트랜잭션 커밋 이후 전송 실패 Push Token 해제를 별도 커밋한다")
	void commitsFailedPushTokenRevocationAfterNotificationCommit() {
		FcmToken existing = fcmTokenJpaRepository.saveAndFlush(
				new FcmToken("user-id", "bad-token", "device-1"));
		NotificationDeliveryService deliveryService = new NotificationDeliveryService(
				(userId, message) -> {
				},
				adapter,
				(token, body) -> {
					throw new IllegalStateException("push delivery failed");
				},
				adapter);
		SpringNotificationDeliverySchedulerAdapter scheduler =
				new SpringNotificationDeliverySchedulerAdapter(deliveryService);
		NotificationDeliveryRequest request = new NotificationDeliveryRequest(
				1L,
				"user-id",
				new NotificationStreamMessage("event-id", null, "payload"),
				"push-body");

		new TransactionTemplate(transactionManager)
				.executeWithoutResult(status -> scheduler.scheduleNotificationDelivery(request));

		assertThat(fcmTokenJpaRepository.findById(existing.getId())).isEmpty();
	}

	@Test
	@DisplayName("사용자 기기의 Push Token 해제를 커밋한다")
	void commitsDevicePushTokenRevocation() {
		FcmToken existing = fcmTokenJpaRepository.saveAndFlush(
				new FcmToken("user-id", "token-1", "device-1"));

		adapter.revokePushTokenForDevice("user-id", "device-1");

		assertThat(fcmTokenJpaRepository.findById(existing.getId())).isEmpty();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class Configuration {

		@Bean
		FcmTokenPersistenceAdapter fcmTokenPersistenceAdapter(FcmTokenJpaRepository repository) {
			return new FcmTokenPersistenceAdapter(repository);
		}
	}
}
