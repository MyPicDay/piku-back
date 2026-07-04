package com.pikume.back.creative.application.port.out;

import com.pikume.back.creative.domain.DiaryImageGeneration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 생성 이력 조회 Outbound Port
 */
public interface LoadGenerationPort {

	record DailyCount(LocalDate date, long count) {
	}

	Optional<DiaryImageGeneration> loadGenerationForDiaryIntegration(Long generationId);

	boolean isGenerationOwnedByUser(Long generationId, String userId);

	List<DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	List<DailyCount> countAllSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	long countAllSuccessfulGenerations();

	long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive);
}
