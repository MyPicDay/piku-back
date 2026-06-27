package com.pikume.back.user.application.service;

import com.pikume.back.user.application.port.out.LoadUserPort;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDashboardStatisticsQueryService")
class UserDashboardStatisticsQueryServiceTest {

	@Mock
	private LoadUserPort loadUserPort;

	@Test
	@DisplayName("누적 회원 수와 탈퇴 여부를 무시한 일간 가입 집계를 사용자 조회 포트에 위임한다")
	void delegatesDashboardStatistics() {
		LocalDate startDate = LocalDate.of(2026, 6, 16);
		LocalDate endDate = LocalDate.of(2026, 6, 22);
		LocalDateTime cutoff = startDate.atStartOfDay();
		given(loadUserPort.countAllMembers()).willReturn(100L);
		given(loadUserPort.countMembersBefore(cutoff)).willReturn(90L);
		given(loadUserPort.countAllSignupMembersByDate(startDate, endDate))
				.willReturn(List.of(new LoadUserPort.DailyCount(startDate, 2)));

		UserDashboardStatisticsQueryService service = new UserDashboardStatisticsQueryService(loadUserPort);

		assertThat(service.countCurrentCumulativeMembers()).isEqualTo(100);
		assertThat(service.countCumulativeMembersBefore(cutoff)).isEqualTo(90);
		assertThat(service.countSignupMembersByDate(startDate, endDate))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.date()).isEqualTo(startDate);
					assertThat(row.count()).isEqualTo(2);
				});
	}
}
