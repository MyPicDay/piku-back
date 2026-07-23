package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.QueryAdminDailyStatisticsPort;
import com.pikume.back.admin.application.port.out.QueryAdminStatisticsSourcePort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminDailyStatistics;
import com.pikume.back.admin.domain.AdminRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsQueryService")
class AdminStatisticsQueryServiceTest {

	@Mock
	private QueryAdminAccountPort queryAdminAccountPort;
	@Mock
	private QueryAdminDailyStatisticsPort queryAdminDailyStatisticsPort;
	@Mock
	private QueryAdminStatisticsSourcePort queryAdminStatisticsSourcePort;
	@Mock
	private AdminDailyStatisticsCalculator adminDailyStatisticsCalculator;

	@Test
	@DisplayName("과거 집계 데이터와 오늘 실시간 데이터를 조합하고 누락 날짜는 0으로 채운다")
	void getStatisticsMergesAggregatedAndLiveRows() {
		LocalDate today = LocalDate.now();
		LocalDate startDate = today.minusDays(2);
		LocalDate yesterday = today.minusDays(1);
		given(queryAdminAccountPort.findAccount("admin-1")).willReturn(Optional.of(admin()));
		given(queryAdminStatisticsSourcePort.countCurrentMembers()).willReturn(42L);
		given(queryAdminDailyStatisticsPort.queryStatisticsPeriod(startDate, today))
				.willReturn(List.of(AdminDailyStatistics.of(
						startDate,
						10,
						20,
						7,
						3,
						4,
						5,
						1,
						2,
						LocalDateTime.now())));
		Map<LocalDate, AdminDailyStatisticsResult> live = new LinkedHashMap<>();
		live.put(today, new AdminDailyStatisticsResult(today, 1, 2, 1, 3, 4, 5, 6, 7));
		given(adminDailyStatisticsCalculator.calculate(startDate, today)).willReturn(live);

		AdminStatisticsResponse response = service().getStatistics("admin-1", startDate, today);

		assertThat(response.currentMemberCount()).isEqualTo(42);
		assertThat(response.dailyStatistics()).hasSize(3);
		assertThat(response.dailyStatistics().get(0).dailyUniqueVisitors()).isEqualTo(10);
		assertThat(response.dailyStatistics().get(1)).isEqualTo(AdminDailyStatisticsResult.zero(yesterday));
		assertThat(response.dailyStatistics().get(2).aiPhotoFailures()).isEqualTo(6);
		assertThat(response.weeklySignupMembers())
				.extracting(AdminSignupBucketResult::signupMembers)
				.satisfies(counts -> assertThat(counts.stream().mapToLong(Long::longValue).sum()).isEqualTo(9L));
	}

	@Test
	@DisplayName("통계 조회 기간은 최대 1년으로 제한한다")
	void getStatisticsRejectsRangeLongerThanOneYear() {
		LocalDate today = LocalDate.now();
		given(queryAdminAccountPort.findAccount("admin-1")).willReturn(Optional.of(admin()));

		assertThatThrownBy(() -> service().getStatistics("admin-1", today.minusYears(1).minusDays(1), today))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.INVALID_REQUEST));
	}

	@Test
	@DisplayName("VIEWER는 통계 CSV를 내보낼 수 없다")
	void getStatisticsCsvRejectsViewer() {
		LocalDate startDate = LocalDate.of(2026, 6, 11);
		LocalDate endDate = LocalDate.of(2026, 6, 17);
		given(queryAdminAccountPort.findAccount("viewer-1")).willReturn(Optional.of(admin(AdminRole.VIEWER)));

		assertThatThrownBy(() -> service().getStatisticsCsv("viewer-1", startDate, endDate))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(AdminErrorCode.FORBIDDEN));
	}

	private AdminStatisticsQueryService service() {
		return new AdminStatisticsQueryService(
				queryAdminAccountPort,
				queryAdminDailyStatisticsPort,
				queryAdminStatisticsSourcePort,
				adminDailyStatisticsCalculator);
	}

	private AdminAccount admin() {
		return admin(AdminRole.VIEWER);
	}

	private AdminAccount admin(AdminRole role) {
		return AdminAccount.invite(
				"admin@pikume.com",
				"관리자1",
				role,
				"temp-hash",
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1));
	}
}
