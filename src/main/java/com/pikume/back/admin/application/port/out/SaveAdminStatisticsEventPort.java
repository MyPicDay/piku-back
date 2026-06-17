package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminStatisticsEvent;

public interface SaveAdminStatisticsEventPort {

	AdminStatisticsEvent save(AdminStatisticsEvent event);
}
