package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminStatisticsEvent;

public interface RecordAdminStatisticsEventPort {

	AdminStatisticsEvent recordStatisticsEvent(AdminStatisticsEvent event);
}
