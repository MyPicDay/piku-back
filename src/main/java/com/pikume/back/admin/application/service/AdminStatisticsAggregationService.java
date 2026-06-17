package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.out.SaveAdminDailyStatisticsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatisticsAggregationService {

	private final AdminDailyStatisticsCalculator adminDailyStatisticsCalculator;
	private final SaveAdminDailyStatisticsPort saveAdminDailyStatisticsPort;

	@Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
	@Transactional
	public void aggregateYesterday() {
		aggregate(LocalDate.now().minusDays(1));
	}

	@Transactional
	public void aggregate(LocalDate date) {
		Map<LocalDate, AdminDailyStatisticsResult> calculated = adminDailyStatisticsCalculator.calculate(date, date);
		AdminDailyStatisticsResult result = calculated.getOrDefault(date, AdminDailyStatisticsResult.zero(date));
		saveAdminDailyStatisticsPort.save(result.toEntity(LocalDateTime.now()));
		log.info("event=admin_daily_statistics_aggregated date={}", date);
	}
}
