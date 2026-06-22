package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.application.service.AdminDailyCount;
import com.pikume.back.admin.domain.AdminStatisticsEventType;

import java.time.LocalDate;
import java.util.List;

public interface QueryAdminStatisticsEventPort {

	List<AdminDailyCount> countEventsByDate(AdminStatisticsEventType eventType, LocalDate startDate, LocalDate endDate);

	List<AdminDailyCount> countDistinctVisitorsByDate(LocalDate startDate, LocalDate endDate);

	List<AdminDailyCount> countDistinctActiveUsersByDate(LocalDate startDate, LocalDate endDate);

	long countDistinctActiveUsers(LocalDate startDate, LocalDate endDate);
}
