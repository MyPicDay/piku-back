package com.pikume.back.notification.application.port.in;

import com.pikume.back.notification.application.dto.RecordNotificationCommand;

public interface RecordNotificationUseCase {

	void recordNotification(RecordNotificationCommand command);
}
