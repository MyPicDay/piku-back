package com.pikume.back.user.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryUserDashboardStatisticsUseCase {

	record DailyCount(LocalDate date, long count) {
	}

	long countCurrentCumulativeMembers();

	long countCumulativeMembersBefore(LocalDateTime cutoffExclusive);

	List<DailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate);

	long countCurrentActiveMembers();

	List<DailyCount> countActiveSignupMembersByDate(LocalDate startDate, LocalDate endDate);
}
