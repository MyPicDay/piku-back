package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.port.in.QueryDiaryDashboardStatisticsUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryDashboardStatisticsQueryService implements QueryDiaryDashboardStatisticsUseCase {

	private final LoadDiaryPort loadDiaryPort;

	@Override
	public long countAllCreatedDiaries() {
		return loadDiaryPort.countAllCreatedDiaries();
	}

	@Override
	public long countCreatedDiariesBefore(LocalDateTime cutoffExclusive) {
		return loadDiaryPort.countCreatedDiariesBefore(cutoffExclusive);
	}

	@Override
	public List<DailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate) {
		return loadDiaryPort.countCreatedDiariesByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}
}
