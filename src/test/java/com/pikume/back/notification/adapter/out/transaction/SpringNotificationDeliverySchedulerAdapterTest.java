package com.pikume.back.notification.adapter.out.transaction;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;
import com.pikume.back.notification.application.dto.NotificationStreamMessage;
import com.pikume.back.notification.application.port.in.DeliverNotificationUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("SpringNotificationDeliverySchedulerAdapter")
class SpringNotificationDeliverySchedulerAdapterTest {

	private final DeliverNotificationUseCase deliverNotificationUseCase =
			mock(DeliverNotificationUseCase.class);
	private final SpringNotificationDeliverySchedulerAdapter adapter =
			new SpringNotificationDeliverySchedulerAdapter(deliverNotificationUseCase);

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("활성 트랜잭션에서는 커밋 완료 후 알림을 전달한다")
	void schedulesDeliveryAfterCommit() {
		NotificationDeliveryRequest request = request();
		TransactionSynchronizationManager.initSynchronization();

		adapter.scheduleNotificationDelivery(request);

		then(deliverNotificationUseCase).should(never()).deliverNotification(request);
		TransactionSynchronization synchronization =
				TransactionSynchronizationManager.getSynchronizations().get(0);
		synchronization.afterCommit();
		then(deliverNotificationUseCase).should().deliverNotification(request);
	}

	@Test
	@DisplayName("활성 트랜잭션이 없으면 알림을 즉시 전달한다")
	void deliversImmediatelyWithoutTransaction() {
		NotificationDeliveryRequest request = request();

		adapter.scheduleNotificationDelivery(request);

		then(deliverNotificationUseCase).should().deliverNotification(request);
	}

	private NotificationDeliveryRequest request() {
		return new NotificationDeliveryRequest(
				1L,
				"receiver-id",
				new NotificationStreamMessage("event-id", null, "payload"),
				"push-body");
	}
}
