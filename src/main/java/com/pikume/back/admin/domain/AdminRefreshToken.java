package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
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
@Table(name = "admin_refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRefreshToken {

	@Id
	@Column(name = "token_hash", length = 64)
	private String tokenHash;

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "admin_id", nullable = false, length = 36)
	private String adminId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AdminRefreshTokenStatus status;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "rotated_at")
	private LocalDateTime rotatedAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "reused_at")
	private LocalDateTime reusedAt;

	private AdminRefreshToken(String tokenHash, String sessionId, String adminId,
			LocalDateTime issuedAt, LocalDateTime expiresAt) {
		this.tokenHash = requireText(tokenHash, "관리자 Refresh Token 해시는 필수입니다.");
		this.sessionId = requireText(sessionId, "관리자 세션 ID는 필수입니다.");
		this.adminId = requireText(adminId, "관리자 ID는 필수입니다.");
		this.issuedAt = requireTime(issuedAt, "관리자 Refresh Token 발급 시각은 필수입니다.");
		this.expiresAt = requireTime(expiresAt, "관리자 Refresh Token 만료 시각은 필수입니다.");
		this.status = AdminRefreshTokenStatus.ACTIVE;
	}

	public static AdminRefreshToken issue(String tokenHash, String sessionId, String adminId,
			LocalDateTime issuedAt, LocalDateTime expiresAt) {
		return new AdminRefreshToken(tokenHash, sessionId, adminId, issuedAt, expiresAt);
	}

	public boolean matchesSession(String sessionId) {
		return this.sessionId.equals(sessionId);
	}

	public boolean belongsTo(String adminId) {
		return this.adminId.equals(adminId);
	}

	public boolean isActiveAt(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		return status == AdminRefreshTokenStatus.ACTIVE && now.isBefore(expiresAt);
	}

	public boolean isRotated() {
		return status == AdminRefreshTokenStatus.ROTATED;
	}

	public void rotate(LocalDateTime now) {
		requireActive();
		this.status = AdminRefreshTokenStatus.ROTATED;
		this.rotatedAt = requireTime(now, "관리자 Refresh Token 회전 시각은 필수입니다.");
	}

	public void revoke(LocalDateTime now) {
		if (status == AdminRefreshTokenStatus.REVOKED) {
			return;
		}
		this.status = AdminRefreshTokenStatus.REVOKED;
		this.revokedAt = requireTime(now, "관리자 Refresh Token 폐기 시각은 필수입니다.");
	}

	public void expire(LocalDateTime now) {
		if (status != AdminRefreshTokenStatus.ACTIVE) {
			return;
		}
		this.status = AdminRefreshTokenStatus.EXPIRED;
		this.revokedAt = requireTime(now, "관리자 Refresh Token 만료 시각은 필수입니다.");
	}

	public void markReused(LocalDateTime now) {
		this.status = AdminRefreshTokenStatus.REUSED;
		this.reusedAt = requireTime(now, "관리자 Refresh Token 재사용 탐지 시각은 필수입니다.");
		this.revokedAt = now;
	}

	private void requireActive() {
		if (status != AdminRefreshTokenStatus.ACTIVE) {
			throw new AdminDomainException("활성 관리자 Refresh Token이 아닙니다.");
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
