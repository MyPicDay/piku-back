package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.dto.AdminDailyCount;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.QueryAdminDashboardSourcePort;
import com.pikume.back.admin.application.port.out.QueryAdminStatisticsEventPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardQueryService")
class AdminDashboardQueryServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDate TODAY = LocalDate.of(2026, 6, 22);
	private static final Clock CLOCK = Clock.fixed(
			Instant.parse("2026-06-22T03:00:00Z"),
			SEOUL);

	@Mock
	private QueryAdminAccountPort queryAdminAccountPort;
	@Mock
	private QueryAdminDashboardSourcePort sourcePort;
	@Mock
	private QueryAdminStatisticsEventPort eventPort;

	@Test
	@DisplayName("서울 날짜 기준 핵심 지표와 고정 기간을 계산한다")
	void getDashboardCalculatesKeyMetricsWithFixedPeriods() {
		given(queryAdminAccountPort.findAccount("admin-1")).willReturn(Optional.of(admin()));
		LocalDate recent30Start = LocalDate.of(2026, 5, 24);
		LocalDate previous30Start = LocalDate.of(2026, 4, 24);
		LocalDate previous30End = LocalDate.of(2026, 5, 23);
		LocalDateTime sevenDayCutoff = LocalDate.of(2026, 6, 16).atStartOfDay();
		given(sourcePort.countCurrentCumulativeMembers()).willReturn(100L);
		given(sourcePort.countCumulativeMembersBefore(sevenDayCutoff)).willReturn(90L);
		given(eventPort.countDistinctActiveUsers(recent30Start, TODAY)).willReturn(30L);
		given(eventPort.countDistinctActiveUsers(previous30Start, previous30End)).willReturn(25L);
		given(sourcePort.countAllSuccessfulAiPhotos()).willReturn(50L);
		given(sourcePort.countSuccessfulAiPhotosBefore(sevenDayCutoff)).willReturn(40L);
		given(sourcePort.countAllCreatedDiaries()).willReturn(200L);
		given(sourcePort.countCreatedDiariesBefore(sevenDayCutoff)).willReturn(180L);

		AdminDashboardResponse response = service().getDashboard("admin-1");

		assertThat(response.keyMetrics()).isEqualTo(new AdminDashboardResponse.KeyMetrics(
				100,
				90,
				30,
				25,
				50,
				40,
				200,
				180));
	}

	@Test
	@DisplayName("최근 7일 일간 데이터는 빈 날짜를 0으로 채우고 AI 성공과 실패를 합산한다")
	void getDashboardZeroFillsDailySeriesAndSummarizesAiPhotos() {
		given(queryAdminAccountPort.findAccount("admin-1")).willReturn(Optional.of(admin()));
		stubKeyMetrics();
		LocalDate startDate = LocalDate.of(2026, 6, 16);
		given(eventPort.countDistinctActiveUsersByDate(startDate, TODAY)).willReturn(List.of(
				new AdminDailyCount(LocalDate.of(2026, 6, 16), 3),
				new AdminDailyCount(LocalDate.of(2026, 6, 18), 5),
				new AdminDailyCount(TODAY, 4)));
		given(sourcePort.countAllSuccessfulAiPhotosByDate(startDate, TODAY)).willReturn(List.of(
				new AdminDailyCount(LocalDate.of(2026, 6, 16), 2),
				new AdminDailyCount(TODAY, 3)));
		given(eventPort.countEventsByDate(AdminStatisticsEventType.AI_PHOTO_FAILURE, startDate, TODAY))
				.willReturn(List.of(
						new AdminDailyCount(LocalDate.of(2026, 6, 17), 1),
						new AdminDailyCount(TODAY, 2)));
		given(sourcePort.countSignupMembersByDate(LocalDate.of(2026, 6, 1), TODAY))
				.willReturn(List.of(
						new AdminDailyCount(LocalDate.of(2026, 6, 16), 2),
						new AdminDailyCount(TODAY, 3)));
		given(sourcePort.countDiaryCreationsByDate(LocalDate.of(2026, 6, 1), TODAY))
				.willReturn(List.of(
						new AdminDailyCount(LocalDate.of(2026, 6, 2), 4),
						new AdminDailyCount(LocalDate.of(2026, 6, 17), 5),
						new AdminDailyCount(TODAY, 6)));
		given(eventPort.countEventsByDate(AdminStatisticsEventType.AI_PHOTO_REQUEST, startDate, TODAY))
				.willReturn(List.of(
						new AdminDailyCount(LocalDate.of(2026, 6, 16), 7),
						new AdminDailyCount(TODAY, 8)));

		AdminDashboardResponse response = service().getDashboard("admin-1");

		assertThat(response.dailyActiveUsers()).hasSize(7);
		assertThat(response.dailyActiveUsers().get(0))
				.isEqualTo(new AdminDashboardResponse.DailyActiveUser(startDate, 3));
		assertThat(response.dailyActiveUsers().get(1))
				.isEqualTo(new AdminDashboardResponse.DailyActiveUser(startDate.plusDays(1), 0));
		assertThat(response.dailyActiveUsers().get(6))
				.isEqualTo(new AdminDashboardResponse.DailyActiveUser(TODAY, 4));
		assertThat(response.dailySummary())
				.zipSatisfy(response.dailyActiveUsers(), (summary, activeUser) -> {
					assertThat(summary.date()).isEqualTo(activeUser.date());
					assertThat(summary.dau()).isEqualTo(activeUser.dau());
				});
		assertThat(response.aiPhotoGeneration())
				.isEqualTo(new AdminDashboardResponse.AiPhotoGeneration(5, 3));
		assertThat(response.dailySummary().get(0))
				.isEqualTo(new AdminDashboardResponse.DailySummary(startDate, 2, 3, 0, 7));
		assertThat(response.dailySummary().get(1))
				.isEqualTo(new AdminDashboardResponse.DailySummary(startDate.plusDays(1), 0, 0, 5, 0));
	}

	@Test
	@DisplayName("최근 4개 ISO 주를 반환하고 현재 주는 일요일까지 표시한다")
	void getDashboardBuildsFourIsoWeekBuckets() {
		given(queryAdminAccountPort.findAccount("admin-1")).willReturn(Optional.of(admin()));
		stubKeyMetrics();
		LocalDate dailyStart = LocalDate.of(2026, 6, 16);
		given(eventPort.countDistinctActiveUsersByDate(dailyStart, TODAY)).willReturn(List.of());
		given(sourcePort.countAllSuccessfulAiPhotosByDate(dailyStart, TODAY)).willReturn(List.of());
		given(eventPort.countEventsByDate(AdminStatisticsEventType.AI_PHOTO_FAILURE, dailyStart, TODAY))
				.willReturn(List.of());
		given(eventPort.countEventsByDate(AdminStatisticsEventType.AI_PHOTO_REQUEST, dailyStart, TODAY))
				.willReturn(List.of());
		given(sourcePort.countSignupMembersByDate(LocalDate.of(2026, 6, 1), TODAY))
				.willReturn(List.of(
						new AdminDailyCount(LocalDate.of(2026, 6, 1), 1),
						new AdminDailyCount(LocalDate.of(2026, 6, 16), 2),
						new AdminDailyCount(TODAY, 3)));
		given(sourcePort.countDiaryCreationsByDate(LocalDate.of(2026, 6, 1), TODAY))
				.willReturn(List.of(
						new AdminDailyCount(LocalDate.of(2026, 6, 2), 4),
						new AdminDailyCount(LocalDate.of(2026, 6, 17), 5),
						new AdminDailyCount(TODAY, 6)));

		AdminDashboardResponse response = service().getDashboard("admin-1");

		assertThat(response.weeklyActivity()).containsExactly(
				new AdminDashboardResponse.WeeklyActivity(
						LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), 1, 4),
				new AdminDashboardResponse.WeeklyActivity(
						LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14), 0, 0),
				new AdminDashboardResponse.WeeklyActivity(
						LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 21), 2, 5),
				new AdminDashboardResponse.WeeklyActivity(
						LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 28), 3, 6));
	}

	@Test
	@DisplayName("인증 관리자 계정이 없으면 미인증 오류를 반환한다")
	void getDashboardRequiresAdmin() {
		given(queryAdminAccountPort.findAccount("missing")).willReturn(Optional.empty());

		assertThatThrownBy(() -> service().getDashboard("missing"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.UNAUTHENTICATED));
	}

	private void stubKeyMetrics() {
		LocalDateTime cutoff = LocalDate.of(2026, 6, 16).atStartOfDay();
		given(sourcePort.countCurrentCumulativeMembers()).willReturn(0L);
		given(sourcePort.countCumulativeMembersBefore(cutoff)).willReturn(0L);
		given(eventPort.countDistinctActiveUsers(LocalDate.of(2026, 5, 24), TODAY)).willReturn(0L);
		given(eventPort.countDistinctActiveUsers(LocalDate.of(2026, 4, 24), LocalDate.of(2026, 5, 23)))
				.willReturn(0L);
		given(sourcePort.countAllSuccessfulAiPhotos()).willReturn(0L);
		given(sourcePort.countSuccessfulAiPhotosBefore(cutoff)).willReturn(0L);
		given(sourcePort.countAllCreatedDiaries()).willReturn(0L);
		given(sourcePort.countCreatedDiariesBefore(cutoff)).willReturn(0L);
	}

	private AdminDashboardQueryService service() {
		return new AdminDashboardQueryService(queryAdminAccountPort, sourcePort, eventPort, CLOCK);
	}

	private AdminAccount admin() {
		return AdminAccount.invite(
				"admin@pikume.com",
				"관리자",
				AdminRole.VIEWER,
				"temp-hash",
				LocalDateTime.now(CLOCK).minusDays(1),
				LocalDateTime.now(CLOCK).plusDays(1));
	}
}
