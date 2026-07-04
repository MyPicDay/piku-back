package com.pikume.back.creative.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryAiPhotoDashboardStatisticsUseCase {

	record DailyCount(LocalDate date, long count) {
	}

	long countAllSuccessfulGenerations();

	long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive);

	List<DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	List<DailyCount> countAllSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);
}
