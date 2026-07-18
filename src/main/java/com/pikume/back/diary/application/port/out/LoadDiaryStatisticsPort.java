package com.pikume.back.diary.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface LoadDiaryStatisticsPort {

	record DailyCount(LocalDate date, long count) {
	}

	long countAllCreated();

	long countCreatedBefore(LocalDateTime cutoffExclusive);

	List<DailyCount> countCreatedByDate(LocalDate startDate, LocalDate endDate);
}
