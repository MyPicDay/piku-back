package com.pikume.back.notification.adapter.out.transaction;

import com.pikume.back.notification.application.dto.NotificationDeliveryRequest;
import com.pikume.back.notification.application.port.in.DeliverNotificationUseCase;
import com.pikume.back.notification.application.port.out.ScheduleNotificationDeliveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class SpringNotificationDeliverySchedulerAdapter implements ScheduleNotificationDeliveryPort {

	private final DeliverNotificationUseCase deliverNotificationUseCase;

	@Override
	public void scheduleNotificationDelivery(NotificationDeliveryRequest request) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deliverNotificationUseCase.deliverNotification(request);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deliverNotificationUseCase.deliverNotification(request);
			}
		});
	}
}
