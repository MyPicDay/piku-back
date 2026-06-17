package com.pikume.back.admin.adapter.in.scheduler;

import com.pikume.back.admin.application.port.in.AdminStatisticsAggregationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminStatisticsAggregationScheduler {

	private final AdminStatisticsAggregationUseCase adminStatisticsAggregationUseCase;

	@Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
	public void run() {
		adminStatisticsAggregationUseCase.aggregateYesterday();
	}
}
