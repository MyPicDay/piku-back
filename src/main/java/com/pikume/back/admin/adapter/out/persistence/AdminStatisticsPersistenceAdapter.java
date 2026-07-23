package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.port.out.QueryAdminDailyStatisticsPort;
import com.pikume.back.admin.application.port.out.QueryAdminStatisticsEventPort;
import com.pikume.back.admin.application.port.out.RecordAdminDailyStatisticsPort;
import com.pikume.back.admin.application.port.out.RecordAdminStatisticsEventPort;
import com.pikume.back.admin.application.dto.AdminDailyCount;
import com.pikume.back.admin.domain.AdminDailyStatistics;
import com.pikume.back.admin.domain.AdminStatisticsEvent;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminStatisticsPersistenceAdapter implements
		RecordAdminStatisticsEventPort,
		QueryAdminStatisticsEventPort,
		QueryAdminDailyStatisticsPort,
		RecordAdminDailyStatisticsPort {

	private final AdminStatisticsEventJpaRepository adminStatisticsEventJpaRepository;
	private final AdminDailyStatisticsJpaRepository adminDailyStatisticsJpaRepository;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public AdminStatisticsEvent recordStatisticsEvent(AdminStatisticsEvent event) {
		return adminStatisticsEventJpaRepository.save(event);
	}

	@Override
	public List<AdminDailyCount> countEventsByDate(AdminStatisticsEventType eventType, LocalDate startDate, LocalDate endDate) {
		return queryDailyCount("""
				SELECT event_date AS metric_date, COUNT(*) AS metric_count
				FROM admin_statistics_events
				WHERE event_type = :eventType
				  AND event_date BETWEEN :startDate AND :endDate
				GROUP BY event_date
				""", params(startDate, endDate).addValue("eventType", eventType.name()));
	}

	@Override
	public List<AdminDailyCount> countDistinctVisitorsByDate(LocalDate startDate, LocalDate endDate) {
		return queryDailyCount("""
				SELECT event_date AS metric_date, COUNT(DISTINCT visitor_key) AS metric_count
				FROM admin_statistics_events
				WHERE event_type = :eventType
				  AND visitor_key IS NOT NULL
				  AND event_date BETWEEN :startDate AND :endDate
				GROUP BY event_date
				""", params(startDate, endDate).addValue("eventType", AdminStatisticsEventType.VISIT.name()));
	}

	@Override
	public List<AdminDailyCount> countDistinctActiveUsersByDate(LocalDate startDate, LocalDate endDate) {
		return queryDailyCount("""
				SELECT event_date AS metric_date, COUNT(DISTINCT user_id) AS metric_count
				FROM admin_statistics_events
				WHERE event_type = :eventType
				  AND user_id IS NOT NULL
				  AND event_date BETWEEN :startDate AND :endDate
				GROUP BY event_date
				""", params(startDate, endDate).addValue("eventType", AdminStatisticsEventType.VISIT.name()));
	}

	@Override
	public long countDistinctActiveUsers(LocalDate startDate, LocalDate endDate) {
		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(DISTINCT user_id)
				FROM admin_statistics_events
				WHERE event_type = :eventType
				  AND user_id IS NOT NULL
				  AND event_date BETWEEN :startDate AND :endDate
				""", params(startDate, endDate).addValue("eventType", AdminStatisticsEventType.VISIT.name()), Long.class);
		return count == null ? 0 : count;
	}

	@Override
	public List<AdminDailyStatistics> queryStatisticsPeriod(LocalDate startDate, LocalDate endDate) {
		return adminDailyStatisticsJpaRepository.findByMetricDateBetween(startDate, endDate);
	}

	@Override
	public AdminDailyStatistics recordDailyStatistics(AdminDailyStatistics statistics) {
		return adminDailyStatisticsJpaRepository.save(statistics);
	}

	private List<AdminDailyCount> queryDailyCount(String sql, MapSqlParameterSource parameters) {
		return jdbcTemplate.query(sql, parameters, (rs, rowNum) ->
				new AdminDailyCount(toLocalDate(rs.getObject("metric_date")), rs.getLong("metric_count")));
	}

	private MapSqlParameterSource params(LocalDate startDate, LocalDate endDate) {
		return new MapSqlParameterSource()
				.addValue("startDate", startDate)
				.addValue("endDate", endDate);
	}

	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		return LocalDate.parse(String.valueOf(value));
	}
}
