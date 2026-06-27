package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminStatisticsResponse;

import java.time.LocalDate;

public interface AdminStatisticsUseCase {

	AdminStatisticsResponse getStatistics(String actorAdminId, LocalDate startDate, LocalDate endDate);

	String getStatisticsCsv(String actorAdminId, LocalDate startDate, LocalDate endDate);
}
