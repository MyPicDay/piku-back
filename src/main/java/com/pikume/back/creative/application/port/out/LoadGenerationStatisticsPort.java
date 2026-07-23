package com.pikume.back.creative.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface LoadGenerationStatisticsPort {

	record DailyCount(LocalDate date, long count) {
	}

	List<DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	List<DailyCount> countAllSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	long countAllSuccessfulGenerations();

	long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive);
}
