package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.port.in.QueryAiPhotoDashboardStatisticsUseCase;
import com.pikume.back.creative.application.port.out.LoadGenerationStatisticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiPhotoDashboardStatisticsQueryService implements QueryAiPhotoDashboardStatisticsUseCase {

	private final LoadGenerationStatisticsPort loadGenerationStatisticsPort;

	@Override
	public long countAllSuccessfulGenerations() {
		return loadGenerationStatisticsPort.countAllSuccessfulGenerations();
	}

	@Override
	public long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive) {
		return loadGenerationStatisticsPort.countSuccessfulGenerationsBefore(cutoffExclusive);
	}

	@Override
	public List<DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate) {
		return loadGenerationStatisticsPort.countSuccessfulGenerationsByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public List<DailyCount> countAllSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate) {
		return loadGenerationStatisticsPort.countAllSuccessfulGenerationsByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}
}
