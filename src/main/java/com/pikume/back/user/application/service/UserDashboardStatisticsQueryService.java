package com.pikume.back.user.application.service;

import com.pikume.back.user.application.port.in.QueryUserDashboardStatisticsUseCase;
import com.pikume.back.user.application.port.out.QueryUserStatisticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDashboardStatisticsQueryService implements QueryUserDashboardStatisticsUseCase {

	private final QueryUserStatisticsPort queryUserStatisticsPort;

	@Override
	public long countCurrentCumulativeMembers() {
		return queryUserStatisticsPort.countAllMembers();
	}

	@Override
	public long countCumulativeMembersBefore(LocalDateTime cutoffExclusive) {
		return queryUserStatisticsPort.countMembersBefore(cutoffExclusive);
	}

	@Override
	public List<DailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return queryUserStatisticsPort.countAllSignupMembersByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public long countCurrentActiveMembers() {
		return queryUserStatisticsPort.countActiveMembers();
	}

	@Override
	public List<DailyCount> countActiveSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return queryUserStatisticsPort.countActiveSignupMembersByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}
}
