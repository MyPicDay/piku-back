package com.pikume.back.diary.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 통계 소비자가 공개 In Port로 전환할 때까지 유지하는 제한된 호환 계약이다.
 */
public interface LoadDiaryPort {

	record DailyCount(LocalDate date, long count) {
	}

	List<DailyCount> countCreatedDiariesByDate(LocalDate startDate, LocalDate endDate);

	long countAllCreatedDiaries();

	long countCreatedDiariesBefore(LocalDateTime cutoffExclusive);

}
