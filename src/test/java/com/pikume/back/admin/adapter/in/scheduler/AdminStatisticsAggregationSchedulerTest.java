package com.pikume.back.admin.adapter.in.scheduler;

import com.pikume.back.admin.application.port.in.AdminStatisticsAggregationUseCase;
import com.pikume.back.admin.application.service.AdminStatisticsAggregationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsAggregationScheduler")
class AdminStatisticsAggregationSchedulerTest {

	@Mock
	private AdminStatisticsAggregationUseCase adminStatisticsAggregationUseCase;

	@Test
	@DisplayName("스케줄러는 어제 관리자 통계 집계를 유스케이스에 위임한다")
	void runDelegatesToUseCase() throws Exception {
		Method run = AdminStatisticsAggregationScheduler.class.getMethod("run");

		new AdminStatisticsAggregationScheduler(adminStatisticsAggregationUseCase).run();

		assertThat(run.getAnnotation(Scheduled.class)).isNotNull();
		then(adminStatisticsAggregationUseCase).should().aggregateYesterday();
	}

	@Test
	@DisplayName("관리자 통계 집계 애플리케이션 서비스는 스케줄링 어노테이션을 직접 소유하지 않는다")
	void applicationServiceDoesNotOwnSchedulingAnnotation() {
		boolean hasScheduledMethod = Arrays.stream(AdminStatisticsAggregationService.class.getDeclaredMethods())
				.anyMatch(method -> method.getAnnotation(Scheduled.class) != null);

		assertThat(hasScheduledMethod).isFalse();
	}
}
