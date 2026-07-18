package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.port.out.LoadDiaryStatisticsPort;
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
@DisplayName("DiaryDashboardStatisticsQueryService")
class DiaryDashboardStatisticsQueryServiceTest {

	@Mock
	private LoadDiaryStatisticsPort loadDiaryPort;

	@Test
	@DisplayName("일기 누적과 일간 작성 집계를 일기 조회 포트에 위임한다")
	void delegatesDashboardStatistics() {
		LocalDate startDate = LocalDate.of(2026, 6, 16);
		LocalDate endDate = LocalDate.of(2026, 6, 22);
		LocalDateTime cutoff = startDate.atStartOfDay();
		given(loadDiaryPort.countAllCreated()).willReturn(200L);
		given(loadDiaryPort.countCreatedBefore(cutoff)).willReturn(180L);
		given(loadDiaryPort.countCreatedByDate(startDate, endDate))
				.willReturn(List.of(new LoadDiaryStatisticsPort.DailyCount(startDate, 3)));

		DiaryDashboardStatisticsQueryService service = new DiaryDashboardStatisticsQueryService(loadDiaryPort);

		assertThat(service.countAllCreatedDiaries()).isEqualTo(200);
		assertThat(service.countCreatedDiariesBefore(cutoff)).isEqualTo(180);
		assertThat(service.countDiaryCreationsByDate(startDate, endDate))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.date()).isEqualTo(startDate);
					assertThat(row.count()).isEqualTo(3);
				});
	}
}
