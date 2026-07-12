package com.pikume.back.user.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryUserStatisticsPort {

	record DailyCount(LocalDate date, long count) {
	}

	long countActiveMembers();

	long countAllMembers();

	long countMembersBefore(LocalDateTime cutoffExclusive);

	List<DailyCount> countActiveSignupMembersByDate(LocalDate startDate, LocalDate endDate);

	List<DailyCount> countAllSignupMembersByDate(LocalDate startDate, LocalDate endDate);
}
