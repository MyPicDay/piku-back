package com.pikume.back.admin.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsPersistenceAdapter")
class AdminStatisticsPersistenceAdapterTest {

	@Mock
	private AdminStatisticsEventJpaRepository eventRepository;
	@Mock
	private AdminDailyStatisticsJpaRepository dailyStatisticsRepository;
	@Mock
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("기간 활성 사용자는 userId가 있는 방문 이벤트를 기간 전체에서 중복 제거한다")
	void countsDistinctActiveUsersAcrossPeriod() {
		LocalDate startDate = LocalDate.of(2026, 5, 24);
		LocalDate endDate = LocalDate.of(2026, 6, 22);
		given(jdbcTemplate.queryForObject(
				argThat(sql -> sql.contains("COUNT(DISTINCT user_id)")
						&& sql.contains("user_id IS NOT NULL")
						&& sql.contains("event_type = :eventType")),
				any(MapSqlParameterSource.class),
				eq(Long.class)))
				.willReturn(12L);

		long result = adapter().countDistinctActiveUsers(startDate, endDate);

		assertThat(result).isEqualTo(12L);
	}

	private AdminStatisticsPersistenceAdapter adapter() {
		return new AdminStatisticsPersistenceAdapter(
				eventRepository,
				dailyStatisticsRepository,
				jdbcTemplate);
	}
}
