package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.port.in.QueryAiPhotoDashboardStatisticsUseCase;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
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

	private final LoadGenerationPort loadGenerationPort;

	@Override
	public long countAllSuccessfulGenerations() {
		return loadGenerationPort.countAllSuccessfulGenerations();
	}

	@Override
	public long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive) {
		return loadGenerationPort.countSuccessfulGenerationsBefore(cutoffExclusive);
	}

	@Override
	public List<DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate) {
		return loadGenerationPort.countSuccessfulGenerationsByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public List<DailyCount> countAllSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate) {
		return loadGenerationPort.countAllSuccessfulGenerationsByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}
}
