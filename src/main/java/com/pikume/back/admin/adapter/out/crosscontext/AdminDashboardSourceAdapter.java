package com.pikume.back.admin.adapter.out.crosscontext;

import com.pikume.back.admin.application.port.out.QueryAdminDashboardSourcePort;
import com.pikume.back.admin.application.service.AdminDailyCount;
import com.pikume.back.creative.application.port.in.QueryAiPhotoDashboardStatisticsUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryDashboardStatisticsUseCase;
import com.pikume.back.user.application.port.in.QueryUserDashboardStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminDashboardSourceAdapter implements QueryAdminDashboardSourcePort {

	private final QueryUserDashboardStatisticsUseCase userStatisticsUseCase;
	private final QueryDiaryDashboardStatisticsUseCase diaryStatisticsUseCase;
	private final QueryAiPhotoDashboardStatisticsUseCase aiPhotoStatisticsUseCase;

	@Override
	public long countCurrentCumulativeMembers() {
		return userStatisticsUseCase.countCurrentCumulativeMembers();
	}

	@Override
	public long countCumulativeMembersBefore(LocalDateTime cutoffExclusive) {
		return userStatisticsUseCase.countCumulativeMembersBefore(cutoffExclusive);
	}

	@Override
	public long countAllSuccessfulAiPhotos() {
		return aiPhotoStatisticsUseCase.countAllSuccessfulGenerations();
	}

	@Override
	public long countSuccessfulAiPhotosBefore(LocalDateTime cutoffExclusive) {
		return aiPhotoStatisticsUseCase.countSuccessfulGenerationsBefore(cutoffExclusive);
	}

	@Override
	public long countAllCreatedDiaries() {
		return diaryStatisticsUseCase.countAllCreatedDiaries();
	}

	@Override
	public long countCreatedDiariesBefore(LocalDateTime cutoffExclusive) {
		return diaryStatisticsUseCase.countCreatedDiariesBefore(cutoffExclusive);
	}

	@Override
	public List<AdminDailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return userStatisticsUseCase.countSignupMembersByDate(startDate, endDate)
				.stream()
				.map(row -> new AdminDailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public List<AdminDailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate) {
		return diaryStatisticsUseCase.countDiaryCreationsByDate(startDate, endDate)
				.stream()
				.map(row -> new AdminDailyCount(row.date(), row.count()))
				.toList();
	}

	@Override
	public List<AdminDailyCount> countAllSuccessfulAiPhotosByDate(LocalDate startDate, LocalDate endDate) {
		return aiPhotoStatisticsUseCase.countAllSuccessfulGenerationsByDate(startDate, endDate)
				.stream()
				.map(row -> new AdminDailyCount(row.date(), row.count()))
				.toList();
	}
}
