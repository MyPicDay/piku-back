package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.port.out.LoadGenerationPort;
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
@DisplayName("AiPhotoDashboardStatisticsQueryService")
class AiPhotoDashboardStatisticsQueryServiceTest {

	@Mock
	private LoadGenerationPort loadGenerationPort;

	@Test
	@DisplayName("AI 생성 성공 누적과 일간 집계를 생성 이력 조회 포트에 위임한다")
	void delegatesDashboardStatistics() {
		LocalDate startDate = LocalDate.of(2026, 6, 16);
		LocalDate endDate = LocalDate.of(2026, 6, 22);
		LocalDateTime cutoff = startDate.atStartOfDay();
		given(loadGenerationPort.countAllSuccessfulGenerations()).willReturn(50L);
		given(loadGenerationPort.countSuccessfulGenerationsBefore(cutoff)).willReturn(40L);
		given(loadGenerationPort.countAllSuccessfulGenerationsByDate(startDate, endDate))
				.willReturn(List.of(new LoadGenerationPort.DailyCount(startDate, 4)));

		AiPhotoDashboardStatisticsQueryService service =
				new AiPhotoDashboardStatisticsQueryService(loadGenerationPort);

		assertThat(service.countAllSuccessfulGenerations()).isEqualTo(50);
		assertThat(service.countSuccessfulGenerationsBefore(cutoff)).isEqualTo(40);
		assertThat(service.countAllSuccessfulGenerationsByDate(startDate, endDate))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.date()).isEqualTo(startDate);
					assertThat(row.count()).isEqualTo(4);
				});
	}
}
