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

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_admin_id", nullable = false, length = 36)
	private String actorAdminId;

	@Column(name = "target_admin_id", nullable = false, length = 36)
	private String targetAdminId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AdminAuditAction action;

	@Column(length = 500)
	private String reason;

	@Column(columnDefinition = "TEXT")
	private String detail;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	private AdminAuditLog(String actorAdminId, String targetAdminId, AdminAuditAction action,
			String reason, String detail, LocalDateTime occurredAt) {
		this.actorAdminId = requireText(actorAdminId, "작업 관리자 ID는 필수입니다.");
		this.targetAdminId = requireText(targetAdminId, "대상 관리자 ID는 필수입니다.");
		this.action = requireAction(action);
		this.reason = normalizeOptional(reason);
		this.detail = normalizeOptional(detail);
		this.occurredAt = requireTime(occurredAt, "관리자 감사 로그 발생 시각은 필수입니다.");
	}

	public static AdminAuditLog record(String actorAdminId, String targetAdminId, AdminAuditAction action,
			String reason, String detail, LocalDateTime occurredAt) {
		return new AdminAuditLog(actorAdminId, targetAdminId, action, reason, detail, occurredAt);
	}

	private static AdminAuditAction requireAction(AdminAuditAction action) {
		if (action == null) {
			throw new AdminDomainException("관리자 감사 로그 액션은 필수입니다.");
		}
		return action;
	}

	private static String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new AdminDomainException(message);
		}
		return value;
	}

	private static LocalDateTime requireTime(LocalDateTime time, String message) {
		if (time == null) {
			throw new AdminDomainException(message);
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
