package com.pikume.back.admin.adapter.out.crosscontext;

import com.pikume.back.admin.application.service.AdminDailyCount;
import com.pikume.back.creative.application.port.in.QueryAiPhotoDashboardStatisticsUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryDashboardStatisticsUseCase;
import com.pikume.back.user.application.port.in.QueryUserDashboardStatisticsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardSourceAdapter")
class AdminDashboardSourceAdapterTest {

	@Mock
	private QueryUserDashboardStatisticsUseCase userStatisticsUseCase;
	@Mock
	private QueryDiaryDashboardStatisticsUseCase diaryStatisticsUseCase;
	@Mock
	private QueryAiPhotoDashboardStatisticsUseCase aiPhotoStatisticsUseCase;

	@Test
	@DisplayName("누적 지표를 각 데이터 소유 컨텍스트에 위임한다")
	void delegatesCumulativeMetrics() {
		LocalDateTime cutoff = LocalDate.of(2026, 6, 16).atStartOfDay();
		given(userStatisticsUseCase.countCurrentCumulativeMembers()).willReturn(100L);
		given(userStatisticsUseCase.countCumulativeMembersBefore(cutoff)).willReturn(90L);
		given(diaryStatisticsUseCase.countAllCreatedDiaries()).willReturn(200L);
		given(diaryStatisticsUseCase.countCreatedDiariesBefore(cutoff)).willReturn(180L);
		given(aiPhotoStatisticsUseCase.countAllSuccessfulGenerations()).willReturn(50L);
		given(aiPhotoStatisticsUseCase.countSuccessfulGenerationsBefore(cutoff)).willReturn(40L);

		AdminDashboardSourceAdapter adapter = adapter();

		assertThat(adapter.countCurrentCumulativeMembers()).isEqualTo(100);
		assertThat(adapter.countCumulativeMembersBefore(cutoff)).isEqualTo(90);
		assertThat(adapter.countAllCreatedDiaries()).isEqualTo(200);
		assertThat(adapter.countCreatedDiariesBefore(cutoff)).isEqualTo(180);
		assertThat(adapter.countAllSuccessfulAiPhotos()).isEqualTo(50);
		assertThat(adapter.countSuccessfulAiPhotosBefore(cutoff)).isEqualTo(40);
	}

	@Test
	@DisplayName("일간 지표를 공통 관리자 일간 카운트로 변환한다")
	void mapsDailyMetrics() {
		LocalDate startDate = LocalDate.of(2026, 6, 16);
		LocalDate endDate = LocalDate.of(2026, 6, 22);
		given(userStatisticsUseCase.countSignupMembersByDate(startDate, endDate))
				.willReturn(List.of(new QueryUserDashboardStatisticsUseCase.DailyCount(startDate, 1)));
		given(diaryStatisticsUseCase.countDiaryCreationsByDate(startDate, endDate))
				.willReturn(List.of(new QueryDiaryDashboardStatisticsUseCase.DailyCount(startDate.plusDays(1), 2)));
		given(aiPhotoStatisticsUseCase.countAllSuccessfulGenerationsByDate(startDate, endDate))
				.willReturn(List.of(new QueryAiPhotoDashboardStatisticsUseCase.DailyCount(endDate, 3)));

		AdminDashboardSourceAdapter adapter = adapter();

		assertThat(adapter.countSignupMembersByDate(startDate, endDate))
				.containsExactly(new AdminDailyCount(startDate, 1));
		assertThat(adapter.countDiaryCreationsByDate(startDate, endDate))
				.containsExactly(new AdminDailyCount(startDate.plusDays(1), 2));
		assertThat(adapter.countAllSuccessfulAiPhotosByDate(startDate, endDate))
				.containsExactly(new AdminDailyCount(endDate, 3));
		then(userStatisticsUseCase).should().countSignupMembersByDate(startDate, endDate);
	}

	private AdminDashboardSourceAdapter adapter() {
		return new AdminDashboardSourceAdapter(
				userStatisticsUseCase,
				diaryStatisticsUseCase,
				aiPhotoStatisticsUseCase);
	}
}
