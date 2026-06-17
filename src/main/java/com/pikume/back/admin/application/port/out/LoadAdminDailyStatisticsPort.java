package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminDailyStatistics;

import java.time.LocalDate;
import java.util.List;

public interface LoadAdminDailyStatisticsPort {

	List<AdminDailyStatistics> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
