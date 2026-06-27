package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminStatisticsUseCase;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.LoadAdminDailyStatisticsPort;
import com.pikume.back.admin.application.port.out.QueryAdminStatisticsSourcePort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminDailyStatistics;
import com.pikume.back.admin.domain.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatisticsQueryService implements AdminStatisticsUseCase {

	private static final ZoneId STATISTICS_ZONE = ZoneId.of("Asia/Seoul");

	private final LoadAdminAccountPort loadAdminAccountPort;
	private final LoadAdminDailyStatisticsPort loadAdminDailyStatisticsPort;
	private final QueryAdminStatisticsSourcePort queryAdminStatisticsSourcePort;
	private final AdminDailyStatisticsCalculator adminDailyStatisticsCalculator;

	@Override
	@Transactional(readOnly = true)
	public AdminStatisticsResponse getStatistics(String actorAdminId, LocalDate startDate, LocalDate endDate) {
		requireAdmin(actorAdminId);
		return statisticsResponse(startDate, endDate);
	}

	@Override
	@Transactional(readOnly = true)
	public String getStatisticsCsv(String actorAdminId, LocalDate startDate, LocalDate endDate) {
		AdminAccount actor = requireAdmin(actorAdminId);
		requireCsvExportRole(actor);
		AdminStatisticsResponse response = statisticsResponse(startDate, endDate);
		StringBuilder csv = new StringBuilder();
		csv.append("date,current_member_count,daily_unique_visitors,total_visits,dau,diary_creations,")
				.append("ai_photo_requests,ai_photo_successes,ai_photo_failures,signup_members\n");
		for (AdminDailyStatisticsResult row : response.dailyStatistics()) {
			csv.append(row.date()).append(',')
					.append(response.currentMemberCount()).append(',')
					.append(row.dailyUniqueVisitors()).append(',')
					.append(row.totalVisits()).append(',')
					.append(row.dau()).append(',')
					.append(row.diaryCreations()).append(',')
					.append(row.aiPhotoRequests()).append(',')
					.append(row.aiPhotoSuccesses()).append(',')
					.append(row.aiPhotoFailures()).append(',')
					.append(row.signupMembers()).append('\n');
		}
		return csv.toString();
	}

	private AdminStatisticsResponse statisticsResponse(LocalDate startDate, LocalDate endDate) {
		Period period = normalizePeriod(startDate, endDate);
		List<AdminDailyStatisticsResult> dailyStatistics = dailyStatistics(period.startDate(), period.endDate());
		return new AdminStatisticsResponse(
				period.startDate(),
				period.endDate(),
				queryAdminStatisticsSourcePort.countCurrentMembers(),
				dailyStatistics,
				weeklySignupMembers(dailyStatistics),
				monthlySignupMembers(dailyStatistics));
	}

	private List<AdminDailyStatisticsResult> dailyStatistics(LocalDate startDate, LocalDate endDate) {
		LocalDate today = LocalDate.now(STATISTICS_ZONE);
		Map<LocalDate, AdminDailyStatisticsResult> calculated = adminDailyStatisticsCalculator.calculate(startDate, endDate);
		Map<LocalDate, AdminDailyStatisticsResult> aggregated = new LinkedHashMap<>();
		for (AdminDailyStatistics statistics : loadAdminDailyStatisticsPort.findByDateBetween(startDate, endDate)) {
			aggregated.put(statistics.getMetricDate(), AdminDailyStatisticsResult.from(statistics));
		}

		List<AdminDailyStatisticsResult> result = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			AdminDailyStatisticsResult row = date.isBefore(today)
					? aggregated.getOrDefault(date, calculated.getOrDefault(date, AdminDailyStatisticsResult.zero(date)))
					: calculated.getOrDefault(date, AdminDailyStatisticsResult.zero(date));
			result.add(row);
		}
		return result;
	}

	private List<AdminSignupBucketResult> weeklySignupMembers(List<AdminDailyStatisticsResult> dailyStatistics) {
		WeekFields weekFields = WeekFields.ISO;
		Map<String, MutableSignupBucket> buckets = new LinkedHashMap<>();
		for (AdminDailyStatisticsResult row : dailyStatistics) {
			int weekBasedYear = row.date().get(weekFields.weekBasedYear());
			int week = row.date().get(weekFields.weekOfWeekBasedYear());
			String key = "%04d-W%02d".formatted(weekBasedYear, week);
			buckets.computeIfAbsent(key, ignored -> new MutableSignupBucket(key, row.date(), row.date()))
					.add(row.date(), row.signupMembers());
		}
		return toBuckets(buckets);
	}

	private List<AdminSignupBucketResult> monthlySignupMembers(List<AdminDailyStatisticsResult> dailyStatistics) {
		Map<String, MutableSignupBucket> buckets = new LinkedHashMap<>();
		for (AdminDailyStatisticsResult row : dailyStatistics) {
			String key = "%04d-%02d".formatted(row.date().getYear(), row.date().getMonthValue());
			buckets.computeIfAbsent(key, ignored -> new MutableSignupBucket(key, row.date(), row.date()))
					.add(row.date(), row.signupMembers());
		}
		return toBuckets(buckets);
	}

	private List<AdminSignupBucketResult> toBuckets(Map<String, MutableSignupBucket> buckets) {
		return buckets.values()
				.stream()
				.sorted(Comparator.comparing(MutableSignupBucket::startDate))
				.map(MutableSignupBucket::toResult)
				.toList();
	}

	private Period normalizePeriod(LocalDate startDate, LocalDate endDate) {
		LocalDate today = LocalDate.now(STATISTICS_ZONE);
		LocalDate normalizedEndDate = endDate == null ? today : endDate;
		LocalDate normalizedStartDate = startDate == null ? normalizedEndDate.minusDays(6) : startDate;
		if (normalizedStartDate.isAfter(normalizedEndDate)) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
		}
		if (normalizedStartDate.plusYears(1).isBefore(normalizedEndDate)) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "통계 조회 기간은 최대 1년입니다.");
		}
		return new Period(normalizedStartDate, normalizedEndDate);
	}

	private AdminAccount requireAdmin(String actorAdminId) {
		return loadAdminAccountPort.findById(actorAdminId)
				.orElseThrow(() -> new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다."));
	}

	private void requireCsvExportRole(AdminAccount actor) {
		if (actor.getRole() != AdminRole.SUPER_ADMIN && actor.getRole() != AdminRole.OPERATOR) {
			throw new AdminException(AdminProblem.FORBIDDEN, "통계 CSV 내보내기 권한이 없습니다.");
		}
	}

	private record Period(LocalDate startDate, LocalDate endDate) {
	}

	private static class MutableSignupBucket {

		private final String bucket;
		private LocalDate startDate;
		private LocalDate endDate;
		private long signupMembers;

		private MutableSignupBucket(String bucket, LocalDate startDate, LocalDate endDate) {
			this.bucket = bucket;
			this.startDate = startDate;
			this.endDate = endDate;
		}

		private void add(LocalDate date, long count) {
			if (date.isBefore(startDate)) {
				startDate = date;
			}
			if (date.isAfter(endDate)) {
				endDate = date;
			}
			signupMembers += count;
		}

		private LocalDate startDate() {
			return startDate;
		}

		private AdminSignupBucketResult toResult() {
			return new AdminSignupBucketResult(bucket, startDate, endDate, signupMembers);
		}
	}
}
