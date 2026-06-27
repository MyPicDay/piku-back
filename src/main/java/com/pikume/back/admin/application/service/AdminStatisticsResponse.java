package com.pikume.back.admin.application.service;

import java.time.LocalDate;
import java.util.List;

public record AdminStatisticsResponse(
		LocalDate startDate,
		LocalDate endDate,
		long currentMemberCount,
		List<AdminDailyStatisticsResult> dailyStatistics,
		List<AdminSignupBucketResult> weeklySignupMembers,
		List<AdminSignupBucketResult> monthlySignupMembers
) {
}
