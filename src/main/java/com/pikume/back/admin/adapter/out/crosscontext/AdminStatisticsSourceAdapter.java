package com.pikume.back.admin.adapter.out.crosscontext;

import com.pikume.back.admin.application.port.out.QueryAdminStatisticsSourcePort;
import com.pikume.back.admin.application.service.AdminDailyCount;
import com.pikume.back.creative.application.port.in.QueryAiPhotoDashboardStatisticsUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.user.application.port.in.QueryUserDashboardStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminStatisticsSourceAdapter implements QueryAdminStatisticsSourcePort {

	private final QueryUserDashboardStatisticsUseCase queryUserDashboardStatisticsUseCase;
	private final LoadDiaryPort loadDiaryPort;
	private final QueryAiPhotoDashboardStatisticsUseCase aiPhotoStatisticsUseCase;

	@Override
	public long countCurrentMembers() {
		return queryUserDashboardStatisticsUseCase.countCurrentActiveMembers();
	}

	@Override
	public List<AdminDailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return queryUserDashboardStatisticsUseCase.countActiveSignupMembersByDate(startDate, endDate)
				.stream()
				.map(row -> new AdminDailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public List<AdminDailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate) {
		return loadDiaryPort.countCreatedDiariesByDate(startDate, endDate)
				.stream()
				.map(row -> new AdminDailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public List<AdminDailyCount> countAiPhotoSuccessesByDate(LocalDate startDate, LocalDate endDate) {
		return aiPhotoStatisticsUseCase.countSuccessfulGenerationsByDate(startDate, endDate)
				.stream()
				.map(row -> new AdminDailyCount(row.date(), row.count()))
				.toList();
	}
}
