package com.pikume.back.admin.application.service;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
		KeyMetrics keyMetrics,
		List<DailyActiveUser> dailyActiveUsers,
		AiPhotoGeneration aiPhotoGeneration,
		List<WeeklyActivity> weeklyActivity,
		List<DailySummary> dailySummary
) {

	public record KeyMetrics(
			long currentCumulativeMemberCount,
			long cumulativeMemberCountSevenDaysAgo,
			long recent30DayActiveUserCount,
			long previous30DayActiveUserCount,
			long currentAiPhotoSuccessCount,
			long aiPhotoSuccessCountSevenDaysAgo,
			long currentDiaryCreationCount,
			long diaryCreationCountSevenDaysAgo
	) {
	}

	public record DailyActiveUser(LocalDate date, long dau) {
	}

	public record AiPhotoGeneration(long successCount, long failureCount) {
	}

	public record WeeklyActivity(
			LocalDate periodStartDate,
			LocalDate periodEndDate,
			long newMemberCount,
			long diaryCreationCount
	) {
	}

	public record DailySummary(
			LocalDate date,
			long newMemberCount,
			long dau,
			long diaryCreationCount,
			long aiPhotoRequestCount
	) {
	}
}
