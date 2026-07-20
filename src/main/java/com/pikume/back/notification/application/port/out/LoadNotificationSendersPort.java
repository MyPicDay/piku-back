package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.readmodel.NotificationSenderView;

import java.util.Map;
import java.util.Set;

public interface LoadNotificationSendersPort {

	Map<String, NotificationSenderView> loadNotificationSenders(Set<String> senderIds);
}
