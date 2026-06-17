package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
import com.pikume.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSession extends BaseEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "admin_id", nullable = false, length = 36)
	private String adminId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AdminSessionStatus status;

	@Column(name = "current_refresh_token_hash", length = 64)
	private String currentRefreshTokenHash;

	@Column(name = "absolute_expires_at", nullable = false)
	private LocalDateTime absoluteExpiresAt;

	@Column(name = "idle_expires_at", nullable = false)
	private LocalDateTime idleExpiresAt;

	@Column(name = "last_rotated_at", nullable = false)
	private LocalDateTime lastRotatedAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "reuse_detected_at")
	private LocalDateTime reuseDetectedAt;

	private AdminSession(String id, String adminId, String currentRefreshTokenHash,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		this.id = requireText(id, "관리자 세션 ID는 필수입니다.");
		this.adminId = requireText(adminId, "관리자 ID는 필수입니다.");
		this.currentRefreshTokenHash = requireText(currentRefreshTokenHash, "관리자 Refresh Token 해시는 필수입니다.");
		this.absoluteExpiresAt = requireTime(absoluteExpiresAt, "관리자 세션 절대 만료 시각은 필수입니다.");
		this.idleExpiresAt = requireTime(idleExpiresAt, "관리자 세션 유휴 만료 시각은 필수입니다.");
		this.lastRotatedAt = requireTime(now, "관리자 세션 생성 시각은 필수입니다.");
		this.status = AdminSessionStatus.ACTIVE;
	}

	public static AdminSession start(String adminId, String currentRefreshTokenHash,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		return new AdminSession(AdminId.newId(), adminId, currentRefreshTokenHash, absoluteExpiresAt, idleExpiresAt, now);
	}

	public static AdminSession start(String sessionId, String adminId, String currentRefreshTokenHash,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		return new AdminSession(sessionId, adminId, currentRefreshTokenHash, absoluteExpiresAt, idleExpiresAt, now);
	}

	public boolean belongsTo(String adminId) {
		return this.adminId.equals(adminId);
	}

	public boolean isActiveAt(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		return status == AdminSessionStatus.ACTIVE
				&& now.isBefore(absoluteExpiresAt)
				&& now.isBefore(idleExpiresAt);
	}

	public void rotate(String newRefreshTokenHash, LocalDateTime newIdleExpiresAt, LocalDateTime now) {
		requireActive();
		this.currentRefreshTokenHash = requireText(newRefreshTokenHash, "새 관리자 Refresh Token 해시는 필수입니다.");
		this.idleExpiresAt = requireTime(newIdleExpiresAt, "새 관리자 세션 유휴 만료 시각은 필수입니다.");
		this.lastRotatedAt = requireTime(now, "관리자 세션 회전 시각은 필수입니다.");
	}

	public void revoke(LocalDateTime now) {
		if (status == AdminSessionStatus.REVOKED) {
			return;
		}
		this.status = AdminSessionStatus.REVOKED;
		this.revokedAt = requireTime(now, "관리자 세션 폐기 시각은 필수입니다.");
	}

	public void expire(LocalDateTime now) {
		if (status != AdminSessionStatus.ACTIVE) {
			return;
		}
		this.status = AdminSessionStatus.EXPIRED;
		this.revokedAt = requireTime(now, "관리자 세션 만료 시각은 필수입니다.");
	}

	public void markReuseDetected(LocalDateTime now) {
		this.status = AdminSessionStatus.REUSE_DETECTED;
		this.reuseDetectedAt = requireTime(now, "관리자 Refresh Token 재사용 탐지 시각은 필수입니다.");
		this.revokedAt = now;
	}

	private void requireActive() {
		if (status != AdminSessionStatus.ACTIVE) {
			throw new AdminDomainException("활성 관리자 세션이 아닙니다.");
		}
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
}
