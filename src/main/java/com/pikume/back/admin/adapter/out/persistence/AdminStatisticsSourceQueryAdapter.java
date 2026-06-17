package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.port.out.QueryAdminStatisticsSourcePort;
import com.pikume.back.admin.application.service.AdminDailyCount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminStatisticsSourceQueryAdapter implements QueryAdminStatisticsSourcePort {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public long countCurrentMembers() {
		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM users
				WHERE deleted_at IS NULL
				""", new MapSqlParameterSource(), Long.class);
		return count == null ? 0L : count;
	}

	@Override
	public List<AdminDailyCount> countSignupMembersByDate(LocalDate startDate, LocalDate endDate) {
		return countCreatedRows("users", startDate, endDate, true);
	}

	@Override
	public List<AdminDailyCount> countDiaryCreationsByDate(LocalDate startDate, LocalDate endDate) {
		return countCreatedRows("diary", startDate, endDate, false);
	}

	@Override
	public List<AdminDailyCount> countAiPhotoSuccessesByDate(LocalDate startDate, LocalDate endDate) {
		return countCreatedRows("diary_image_generation", startDate, endDate, true);
	}

	private List<AdminDailyCount> countCreatedRows(String tableName, LocalDate startDate, LocalDate endDate,
			boolean excludeDeleted) {
		String deletedClause = excludeDeleted ? "AND deleted_at IS NULL" : "";
		String sql = """
				SELECT CAST(created_at AS DATE) AS metric_date, COUNT(*) AS metric_count
				FROM %s
				WHERE created_at >= :startDateTime
				  AND created_at < :endExclusiveDateTime
				  %s
				GROUP BY CAST(created_at AS DATE)
				""".formatted(tableName, deletedClause);
		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("startDateTime", startDate.atStartOfDay())
				.addValue("endExclusiveDateTime", endDate.plusDays(1).atStartOfDay());
		return jdbcTemplate.query(sql, parameters, (rs, rowNum) ->
				new AdminDailyCount(toLocalDate(rs.getObject("metric_date")), rs.getLong("metric_count")));
	}

	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		if (value instanceof LocalDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		return LocalDate.parse(String.valueOf(value));
	}
}
