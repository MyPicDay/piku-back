package com.pikume.back.notification.application.port.out;

import com.pikume.back.notification.application.readmodel.NotificationDiaryContextView;

import java.util.Map;
import java.util.Set;

public interface LoadNotificationDiaryContextsPort {

	Map<Long, NotificationDiaryContextView> loadNotificationDiaryContexts(Set<Long> diaryIds);
}
