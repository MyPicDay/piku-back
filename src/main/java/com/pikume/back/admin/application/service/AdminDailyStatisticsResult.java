package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminDailyStatistics;

import java.time.LocalDate;

public record AdminDailyStatisticsResult(
		LocalDate date,
		long dailyUniqueVisitors,
		long totalVisits,
		long dau,
		long diaryCreations,
		long aiPhotoRequests,
		long aiPhotoSuccesses,
		long aiPhotoFailures,
		long signupMembers
) {

	public static AdminDailyStatisticsResult zero(LocalDate date) {
		return new AdminDailyStatisticsResult(date, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	public static AdminDailyStatisticsResult from(AdminDailyStatistics statistics) {
		return new AdminDailyStatisticsResult(
				statistics.getMetricDate(),
				statistics.getDailyUniqueVisitors(),
				statistics.getTotalVisits(),
				statistics.getDau(),
				statistics.getDiaryCreations(),
				statistics.getAiPhotoRequests(),
				statistics.getAiPhotoSuccesses(),
				statistics.getAiPhotoFailures(),
				statistics.getSignupMembers());
	}

	public AdminDailyStatistics toEntity(java.time.LocalDateTime aggregatedAt) {
		return AdminDailyStatistics.of(
				date,
				dailyUniqueVisitors,
				totalVisits,
				dau,
				diaryCreations,
				aiPhotoRequests,
				aiPhotoSuccesses,
				aiPhotoFailures,
				signupMembers,
				aggregatedAt);
	}
}
