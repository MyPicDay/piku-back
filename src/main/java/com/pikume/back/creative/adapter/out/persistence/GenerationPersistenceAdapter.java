package com.pikume.back.creative.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.creative.application.port.out.LoadGenerationPort;
import com.pikume.back.creative.application.port.out.SaveGenerationPort;
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
public class GenerationPersistenceAdapter implements LoadGenerationPort, SaveGenerationPort {

	private final DiaryImageGenerationJpaRepository repository;

	@Override
	public Optional<DiaryImageGeneration> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	public List<DiaryImageGeneration> findByDiaryIdIsNull() {
		return repository.findByDiaryIdIsNull();
	}

	@Override
	public Optional<DiaryImageGeneration> findByUserIdAndFilePath(String userId, String filePath) {
		return repository.findByUserIdAndFilePath(userId, filePath);
	}

	@Override
	public boolean existsByIdAndUserId(Long id, String userId) {
		return repository.existsByIdAndUserId(id, userId);
	}

	@Override
	public List<LoadGenerationPort.DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate) {
		return repository.countSuccessfulGenerationsByDate(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadGenerationPort.DailyCount(toLocalDate(row.getMetricDate()), row.getMetricCount()))
				.toList();
	}

	@Override
	public List<LoadGenerationPort.DailyCount> countAllSuccessfulGenerationsByDate(
			LocalDate startDate,
			LocalDate endDate) {
		return repository.countAllSuccessfulGenerationsByDate(
						startDate.atStartOfDay(),
						endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadGenerationPort.DailyCount(toLocalDate(row.getMetricDate()), row.getMetricCount()))
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
	public DiaryImageGeneration save(DiaryImageGeneration generation) {
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
