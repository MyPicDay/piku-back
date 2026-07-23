package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.dto.AdminDailyCount;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.AdminDashboardUseCase;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.QueryAdminDashboardSourcePort;
import com.pikume.back.admin.application.port.out.QueryAdminStatisticsEventPort;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardQueryService implements AdminDashboardUseCase {

	private static final ZoneId STATISTICS_ZONE = ZoneId.of("Asia/Seoul");

	private final QueryAdminAccountPort queryAdminAccountPort;
	private final QueryAdminDashboardSourcePort sourcePort;
	private final QueryAdminStatisticsEventPort eventPort;
	private final Clock clock;

	@Autowired
	public AdminDashboardQueryService(
			QueryAdminAccountPort queryAdminAccountPort,
			QueryAdminDashboardSourcePort sourcePort,
			QueryAdminStatisticsEventPort eventPort) {
		this(queryAdminAccountPort, sourcePort, eventPort, Clock.system(STATISTICS_ZONE));
	}

	AdminDashboardQueryService(
			QueryAdminAccountPort queryAdminAccountPort,
			QueryAdminDashboardSourcePort sourcePort,
			QueryAdminStatisticsEventPort eventPort,
			Clock clock) {
		this.queryAdminAccountPort = queryAdminAccountPort;
		this.sourcePort = sourcePort;
		this.eventPort = eventPort;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public AdminDashboardResponse getDashboard(String actorAdminId) {
		requireAdmin(actorAdminId);
		LocalDate today = LocalDate.now(clock);
		LocalDate dailyStart = today.minusDays(6);
		LocalDate recent30Start = today.minusDays(29);
		LocalDate previous30End = recent30Start.minusDays(1);
		LocalDate previous30Start = previous30End.minusDays(29);
		LocalDate fourWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(3);
		LocalDateTime sevenDayCutoff = dailyStart.atStartOfDay();

		Map<LocalDate, Long> dau = counts(eventPort.countDistinctActiveUsersByDate(dailyStart, today));
		Map<LocalDate, Long> aiSuccesses = counts(sourcePort.countAllSuccessfulAiPhotosByDate(dailyStart, today));
		Map<LocalDate, Long> aiFailures = counts(eventPort.countEventsByDate(
				AdminStatisticsEventType.AI_PHOTO_FAILURE, dailyStart, today));
		Map<LocalDate, Long> signupMembers = counts(sourcePort.countSignupMembersByDate(fourWeekStart, today));
		Map<LocalDate, Long> diaryCreations = counts(sourcePort.countDiaryCreationsByDate(fourWeekStart, today));
		Map<LocalDate, Long> aiRequests = counts(eventPort.countEventsByDate(
				AdminStatisticsEventType.AI_PHOTO_REQUEST, dailyStart, today));

		return new AdminDashboardResponse(
				new AdminDashboardResponse.KeyMetrics(
						sourcePort.countCurrentCumulativeMembers(),
						sourcePort.countCumulativeMembersBefore(sevenDayCutoff),
						eventPort.countDistinctActiveUsers(recent30Start, today),
						eventPort.countDistinctActiveUsers(previous30Start, previous30End),
						sourcePort.countAllSuccessfulAiPhotos(),
						sourcePort.countSuccessfulAiPhotosBefore(sevenDayCutoff),
						sourcePort.countAllCreatedDiaries(),
						sourcePort.countCreatedDiariesBefore(sevenDayCutoff)),
				dailyActiveUsers(dailyStart, today, dau),
				new AdminDashboardResponse.AiPhotoGeneration(sum(aiSuccesses), sum(aiFailures)),
				weeklyActivity(fourWeekStart, signupMembers, diaryCreations),
				dailySummary(dailyStart, today, signupMembers, dau, diaryCreations, aiRequests));
	}

	private List<AdminDashboardResponse.DailyActiveUser> dailyActiveUsers(
			LocalDate startDate,
			LocalDate endDate,
			Map<LocalDate, Long> dau) {
		List<AdminDashboardResponse.DailyActiveUser> result = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			result.add(new AdminDashboardResponse.DailyActiveUser(date, dau.getOrDefault(date, 0L)));
		}
		return result;
	}

	private List<AdminDashboardResponse.WeeklyActivity> weeklyActivity(
			LocalDate firstWeekStart,
			Map<LocalDate, Long> signupMembers,
			Map<LocalDate, Long> diaryCreations) {
		List<AdminDashboardResponse.WeeklyActivity> result = new ArrayList<>();
		for (int week = 0; week < 4; week++) {
			LocalDate startDate = firstWeekStart.plusWeeks(week);
			LocalDate endDate = startDate.plusDays(6);
			result.add(new AdminDashboardResponse.WeeklyActivity(
					startDate,
					endDate,
					sumBetween(signupMembers, startDate, endDate),
					sumBetween(diaryCreations, startDate, endDate)));
		}
		return result;
	}

	private List<AdminDashboardResponse.DailySummary> dailySummary(
			LocalDate startDate,
			LocalDate endDate,
			Map<LocalDate, Long> signupMembers,
			Map<LocalDate, Long> dau,
			Map<LocalDate, Long> diaryCreations,
			Map<LocalDate, Long> aiRequests) {
		List<AdminDashboardResponse.DailySummary> result = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			result.add(new AdminDashboardResponse.DailySummary(
					date,
					signupMembers.getOrDefault(date, 0L),
					dau.getOrDefault(date, 0L),
					diaryCreations.getOrDefault(date, 0L),
					aiRequests.getOrDefault(date, 0L)));
		}
		return result;
	}

	private Map<LocalDate, Long> counts(List<AdminDailyCount> counts) {
		Map<LocalDate, Long> result = new LinkedHashMap<>();
		for (AdminDailyCount count : counts) {
			result.merge(count.date(), count.count(), Long::sum);
		}
		return result;
	}

	private long sum(Map<LocalDate, Long> counts) {
		return counts.values().stream().mapToLong(Long::longValue).sum();
	}

	private long sumBetween(Map<LocalDate, Long> counts, LocalDate startDate, LocalDate endDate) {
		return counts.entrySet().stream()
				.filter(entry -> !entry.getKey().isBefore(startDate) && !entry.getKey().isAfter(endDate))
				.mapToLong(Map.Entry::getValue)
				.sum();
	}

	private void requireAdmin(String actorAdminId) {
		queryAdminAccountPort.findAccount(actorAdminId)
				.orElseThrow(() -> new AdminException(AdminErrorCode.UNAUTHENTICATED, "관리자 인증이 필요합니다."));
	}
}
