package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.readmodel.NotificationSummaryView;

public interface LoadNotificationSummaryPort {

	NotificationSummaryView loadNotificationSummary(String receiverId);
}
