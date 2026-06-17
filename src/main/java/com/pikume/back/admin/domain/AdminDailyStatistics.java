package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_daily_statistics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDailyStatistics {

	@Id
	@Column(name = "metric_date")
	private LocalDate metricDate;

	@Column(name = "daily_unique_visitors", nullable = false)
	private long dailyUniqueVisitors;

	@Column(name = "total_visits", nullable = false)
	private long totalVisits;

	@Column(nullable = false)
	private long dau;

	@Column(name = "diary_creations", nullable = false)
	private long diaryCreations;

	@Column(name = "ai_photo_requests", nullable = false)
	private long aiPhotoRequests;

	@Column(name = "ai_photo_successes", nullable = false)
	private long aiPhotoSuccesses;

	@Column(name = "ai_photo_failures", nullable = false)
	private long aiPhotoFailures;

	@Column(name = "signup_members", nullable = false)
	private long signupMembers;

	@Column(name = "aggregated_at", nullable = false)
	private LocalDateTime aggregatedAt;

	private AdminDailyStatistics(LocalDate metricDate, long dailyUniqueVisitors, long totalVisits, long dau,
			long diaryCreations, long aiPhotoRequests, long aiPhotoSuccesses, long aiPhotoFailures,
			long signupMembers, LocalDateTime aggregatedAt) {
		this.metricDate = requireDate(metricDate);
		this.dailyUniqueVisitors = requireNonNegative(dailyUniqueVisitors);
		this.totalVisits = requireNonNegative(totalVisits);
		this.dau = requireNonNegative(dau);
		this.diaryCreations = requireNonNegative(diaryCreations);
		this.aiPhotoRequests = requireNonNegative(aiPhotoRequests);
		this.aiPhotoSuccesses = requireNonNegative(aiPhotoSuccesses);
		this.aiPhotoFailures = requireNonNegative(aiPhotoFailures);
		this.signupMembers = requireNonNegative(signupMembers);
		this.aggregatedAt = requireTime(aggregatedAt);
	}

	public static AdminDailyStatistics of(LocalDate metricDate, long dailyUniqueVisitors, long totalVisits, long dau,
			long diaryCreations, long aiPhotoRequests, long aiPhotoSuccesses, long aiPhotoFailures,
			long signupMembers, LocalDateTime aggregatedAt) {
		return new AdminDailyStatistics(
				metricDate,
				dailyUniqueVisitors,
				totalVisits,
				dau,
				diaryCreations,
				aiPhotoRequests,
				aiPhotoSuccesses,
				aiPhotoFailures,
				signupMembers,
				aggregatedAt);
	}

	private static LocalDate requireDate(LocalDate date) {
		if (date == null) {
			throw new AdminDomainException("통계 집계 날짜는 필수입니다.");
		}
		return date;
	}

	private static LocalDateTime requireTime(LocalDateTime time) {
		if (time == null) {
			throw new AdminDomainException("통계 집계 시각은 필수입니다.");
		}
		return time;
	}

	private static long requireNonNegative(long value) {
		if (value < 0) {
			throw new AdminDomainException("통계 값은 음수일 수 없습니다.");
		}
		return value;
	}
}
