package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.domain.AdminStatisticsEventType;

public interface RecordAdminStatisticsEventUseCase {

	void record(AdminStatisticsEventType eventType, String userId, String visitorKey);
}
