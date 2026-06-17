package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminDailyStatistics;

public interface SaveAdminDailyStatisticsPort {

	AdminDailyStatistics save(AdminDailyStatistics statistics);
}
