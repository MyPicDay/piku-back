package com.pikume.back.admin.application.port.in;

import java.time.LocalDate;

public interface AdminStatisticsAggregationUseCase {

	void aggregateYesterday();

	void aggregate(LocalDate date);
}
