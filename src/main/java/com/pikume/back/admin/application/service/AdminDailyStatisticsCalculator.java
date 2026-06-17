package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.out.QueryAdminStatisticsEventPort;
import com.pikume.back.admin.application.port.out.QueryAdminStatisticsSourcePort;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class AdminDailyStatisticsCalculator {

	private final QueryAdminStatisticsEventPort queryAdminStatisticsEventPort;
	private final QueryAdminStatisticsSourcePort queryAdminStatisticsSourcePort;

	public Map<LocalDate, AdminDailyStatisticsResult> calculate(LocalDate startDate, LocalDate endDate) {
		Map<LocalDate, MutableDailyStatistics> statistics = zeroFilled(startDate, endDate);

		apply(statistics, queryAdminStatisticsEventPort.countDistinctVisitorsByDate(startDate, endDate),
				MutableDailyStatistics::withDailyUniqueVisitors);
		apply(statistics, queryAdminStatisticsEventPort.countEventsByDate(
				AdminStatisticsEventType.VISIT, startDate, endDate), MutableDailyStatistics::withTotalVisits);
		apply(statistics, queryAdminStatisticsEventPort.countDistinctActiveUsersByDate(startDate, endDate),
				MutableDailyStatistics::withDau);
		apply(statistics, queryAdminStatisticsSourcePort.countDiaryCreationsByDate(startDate, endDate),
				MutableDailyStatistics::withDiaryCreations);
		apply(statistics, queryAdminStatisticsEventPort.countEventsByDate(
				AdminStatisticsEventType.AI_PHOTO_REQUEST, startDate, endDate), MutableDailyStatistics::withAiPhotoRequests);
		apply(statistics, queryAdminStatisticsSourcePort.countAiPhotoSuccessesByDate(startDate, endDate),
				MutableDailyStatistics::withAiPhotoSuccesses);
		apply(statistics, queryAdminStatisticsEventPort.countEventsByDate(
				AdminStatisticsEventType.AI_PHOTO_FAILURE, startDate, endDate), MutableDailyStatistics::withAiPhotoFailures);
		apply(statistics, queryAdminStatisticsSourcePort.countSignupMembersByDate(startDate, endDate),
				MutableDailyStatistics::withSignupMembers);

		Map<LocalDate, AdminDailyStatisticsResult> result = new LinkedHashMap<>();
		statistics.forEach((date, value) -> result.put(date, value.toResult()));
		return result;
	}

	private Map<LocalDate, MutableDailyStatistics> zeroFilled(LocalDate startDate, LocalDate endDate) {
		Map<LocalDate, MutableDailyStatistics> statistics = new LinkedHashMap<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			statistics.put(date, new MutableDailyStatistics(date));
		}
		return statistics;
	}

	private void apply(Map<LocalDate, MutableDailyStatistics> target, List<AdminDailyCount> counts,
			BiFunction<MutableDailyStatistics, Long, MutableDailyStatistics> updater) {
		for (AdminDailyCount count : counts) {
			MutableDailyStatistics daily = target.get(count.date());
			if (daily != null) {
				updater.apply(daily, count.count());
			}
		}
	}

	private static class MutableDailyStatistics {

		private final LocalDate date;
		private long dailyUniqueVisitors;
		private long totalVisits;
		private long dau;
		private long diaryCreations;
		private long aiPhotoRequests;
		private long aiPhotoSuccesses;
		private long aiPhotoFailures;
		private long signupMembers;

		private MutableDailyStatistics(LocalDate date) {
			this.date = date;
		}

		private MutableDailyStatistics withDailyUniqueVisitors(long value) {
			this.dailyUniqueVisitors = value;
			return this;
		}

		private MutableDailyStatistics withTotalVisits(long value) {
			this.totalVisits = value;
			return this;
		}

		private MutableDailyStatistics withDau(long value) {
			this.dau = value;
			return this;
		}

		private MutableDailyStatistics withDiaryCreations(long value) {
			this.diaryCreations = value;
			return this;
		}

		private MutableDailyStatistics withAiPhotoRequests(long value) {
			this.aiPhotoRequests = value;
			return this;
		}

		private MutableDailyStatistics withAiPhotoSuccesses(long value) {
			this.aiPhotoSuccesses = value;
			return this;
		}

		private MutableDailyStatistics withAiPhotoFailures(long value) {
			this.aiPhotoFailures = value;
			return this;
		}

		private MutableDailyStatistics withSignupMembers(long value) {
			this.signupMembers = value;
			return this;
		}

		private AdminDailyStatisticsResult toResult() {
			return new AdminDailyStatisticsResult(
					date,
					dailyUniqueVisitors,
					totalVisits,
					dau,
					diaryCreations,
					aiPhotoRequests,
					aiPhotoSuccesses,
					aiPhotoFailures,
					signupMembers);
		}
	}
}
