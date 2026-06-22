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

	Optional<DiaryImageGeneration> findById(Long id);

	List<DiaryImageGeneration> findByDiaryIdIsNull();

	Optional<DiaryImageGeneration> findByUserIdAndFilePath(String userId, String filePath);

	boolean existsByIdAndUserId(Long id, String userId);

	List<DailyCount> countSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	List<DailyCount> countAllSuccessfulGenerationsByDate(LocalDate startDate, LocalDate endDate);

	long countAllSuccessfulGenerations();

	long countSuccessfulGenerationsBefore(LocalDateTime cutoffExclusive);
}
