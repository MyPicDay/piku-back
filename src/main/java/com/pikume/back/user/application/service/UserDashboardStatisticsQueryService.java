package com.pikume.back.user.application.service;

import com.pikume.back.user.application.port.in.QueryUserDashboardStatisticsUseCase;
import com.pikume.back.user.application.port.out.LoadUserPort;
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

	private final LoadUserPort loadUserPort;

	@Override
	public long countCurrentCumulativeMembers() {
		return loadUserPort.countAllMembers();
	}

	@Override
	public long countCumulativeMembersBefore(LocalDateTime cutoffExclusive) {
		return loadUserPort.countMembersBefore(cutoffExclusive);
	}

	@Override
	public List<DailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return loadUserPort.countAllSignupMembersByDate(startDate, endDate)
				.stream()
				.map(row -> new DailyCount(row.date(), row.count()))
				.toList();
	}
}
