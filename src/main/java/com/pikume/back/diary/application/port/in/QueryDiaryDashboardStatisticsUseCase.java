package com.pikume.back.diary.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryDiaryDashboardStatisticsUseCase {

	record DailyCount(LocalDate date, long count) {
	}

	long countAllCreatedDiaries();

	long countCreatedDiariesBefore(LocalDateTime cutoffExclusive);

	List<DailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate);
}
