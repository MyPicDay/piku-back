package com.pikume.back.creative.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.port.out.LoadGenerationForDiaryPort;
import com.pikume.back.creative.application.port.out.LoadGenerationStatisticsPort;
import com.pikume.back.creative.application.port.out.RecordGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 생성 이력 Persistence Adapter
 */
@Component
@RequiredArgsConstructor
public class GenerationPersistenceAdapter
		implements LoadGenerationForDiaryPort, LoadGenerationStatisticsPort, RecordGenerationPort {

	private final DiaryImageGenerationJpaRepository repository;

	@Override
	public Optional<DiaryImageGeneration> loadGenerationForDiary(Long generationId) {
		return repository.findById(generationId);
	}

	@Override
	public boolean isGenerationAvailableForDiary(Long generationId, String userId) {
		return repository.existsByIdAndUserIdAndDiaryIdIsNullAndDeletedAtIsNull(generationId, userId);
	}

	@Override
	public List<LoadGenerationStatisticsPort.DailyCount> countSuccessfulGenerationsByDate(
			LocalDate startDate,
			LocalDate endDate
	) {
		return repository.countSuccessfulGenerationsByDate(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadGenerationStatisticsPort.DailyCount(
						toLocalDate(row.getMetricDate()),
						row.getMetricCount()))
				.toList();
	}

	@Override
	public List<LoadGenerationStatisticsPort.DailyCount> countAllSuccessfulGenerationsByDate(
			LocalDate startDate,
			LocalDate endDate) {
		return repository.countAllSuccessfulGenerationsByDate(
						startDate.atStartOfDay(),
						endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadGenerationStatisticsPort.DailyCount(
						toLocalDate(row.getMetricDate()),
						row.getMetricCount()))
				.toList();
	}

	@Override
	public long countAllSuccessfulGenerations() {
		return repository.count();
	}

	@Override
	public long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive) {
		return repository.countByCreatedAtBefore(cutoffExclusive);
	}

	@Override
	public DiaryImageGeneration recordGeneration(DiaryImageGeneration generation) {
		return repository.save(generation);
	}

	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		if (value instanceof LocalDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		return LocalDate.parse(String.valueOf(value));
	}
}
