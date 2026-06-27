package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_statistics_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminStatisticsEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private AdminStatisticsEventType eventType;

	@Column(name = "event_date", nullable = false)
	private LocalDate eventDate;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "user_id", length = 36)
	private String userId;

	@Column(name = "visitor_key", length = 64)
	private String visitorKey;

	private AdminStatisticsEvent(AdminStatisticsEventType eventType, LocalDate eventDate,
			LocalDateTime occurredAt, String userId, String visitorKey) {
		this.eventType = requireEventType(eventType);
		this.eventDate = requireDate(eventDate);
		this.occurredAt = requireTime(occurredAt);
		this.userId = normalizeOptional(userId);
		this.visitorKey = normalizeOptional(visitorKey);
	}

	public static AdminStatisticsEvent record(AdminStatisticsEventType eventType, LocalDateTime occurredAt,
			String userId, String visitorKey) {
		LocalDateTime normalizedOccurredAt = requireTime(occurredAt);
		return new AdminStatisticsEvent(eventType, normalizedOccurredAt.toLocalDate(), normalizedOccurredAt, userId, visitorKey);
	}

	private static AdminStatisticsEventType requireEventType(AdminStatisticsEventType eventType) {
		if (eventType == null) {
			throw new AdminDomainException("통계 이벤트 타입은 필수입니다.");
		}
		return eventType;
	}

	private static LocalDate requireDate(LocalDate date) {
		if (date == null) {
			throw new AdminDomainException("통계 이벤트 날짜는 필수입니다.");
		}
		return date;
	}

	private static LocalDateTime requireTime(LocalDateTime time) {
		if (time == null) {
			throw new AdminDomainException("통계 이벤트 발생 시각은 필수입니다.");
		}
		return time;
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
