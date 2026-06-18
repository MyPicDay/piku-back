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

	@Column(name = "admin_id", length = 36)
	private String adminId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AdminSessionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AdminSessionPhase phase;

	@Column(name = "session_token_hash", nullable = false, unique = true, length = 64)
	private String sessionTokenHash;

	@Column(name = "csrf_token_hash", nullable = false, length = 64)
	private String csrfTokenHash;

	@Column(name = "authentication_version", nullable = false)
	private long authenticationVersion;

	@Column(name = "absolute_expires_at", nullable = false)
	private LocalDateTime absoluteExpiresAt;

	@Column(name = "idle_expires_at", nullable = false)
	private LocalDateTime idleExpiresAt;

	@Column(name = "last_activity_at", nullable = false)
	private LocalDateTime lastActivityAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	private AdminSession(String id, String sessionTokenHash, String csrfTokenHash,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		this.id = requireText(id, "관리자 세션 ID는 필수입니다.");
		this.sessionTokenHash = requireText(sessionTokenHash, "관리자 세션 토큰 해시는 필수입니다.");
		this.csrfTokenHash = requireText(csrfTokenHash, "관리자 CSRF 토큰 해시는 필수입니다.");
		this.absoluteExpiresAt = requireTime(absoluteExpiresAt, "관리자 세션 절대 만료 시각은 필수입니다.");
		this.idleExpiresAt = requireTime(idleExpiresAt, "관리자 세션 유휴 만료 시각은 필수입니다.");
		this.lastActivityAt = requireTime(now, "관리자 세션 생성 시각은 필수입니다.");
		this.status = AdminSessionStatus.ACTIVE;
		this.phase = AdminSessionPhase.ANONYMOUS;
	}

	public static AdminSession startAnonymous(String sessionTokenHash, String csrfTokenHash,
			LocalDateTime now, LocalDateTime expiresAt) {
		return new AdminSession(AdminId.newId(), sessionTokenHash, csrfTokenHash, expiresAt, expiresAt, now);
	}

	public void bindAdmin(String adminId, long authenticationVersion, AdminSessionPhase nextPhase,
			LocalDateTime expiresAt, LocalDateTime now) {
		requireActive();
		requirePhase(AdminSessionPhase.ANONYMOUS);
		this.adminId = requireText(adminId, "관리자 ID는 필수입니다.");
		this.authenticationVersion = requireVersion(authenticationVersion);
		this.phase = requirePhaseValue(nextPhase);
		this.absoluteExpiresAt = requireTime(expiresAt, "사전 세션 만료 시각은 필수입니다.");
		this.idleExpiresAt = expiresAt;
		this.lastActivityAt = requireTime(now, "사전 세션 변경 시각은 필수입니다.");
	}

	public void advance(AdminSessionPhase expectedPhase, AdminSessionPhase nextPhase, LocalDateTime now) {
		requireActive();
		requirePhase(expectedPhase);
		this.phase = requirePhaseValue(nextPhase);
		this.lastActivityAt = requireTime(now, "관리자 인증 단계 변경 시각은 필수입니다.");
	}

	public void authenticate(String newSessionTokenHash, String newCsrfTokenHash, long authenticationVersion,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		requireActive();
		if (phase != AdminSessionPhase.LOGIN_VERIFY_OTP && phase != AdminSessionPhase.ONBOARDING_VERIFY_OTP) {
			throw new AdminDomainException("OTP 검증 단계에서만 관리자 인증을 완료할 수 있습니다.");
		}
		this.sessionTokenHash = requireText(newSessionTokenHash, "새 관리자 세션 토큰 해시는 필수입니다.");
		this.csrfTokenHash = requireText(newCsrfTokenHash, "새 관리자 CSRF 토큰 해시는 필수입니다.");
		this.authenticationVersion = requireVersion(authenticationVersion);
		this.absoluteExpiresAt = requireTime(absoluteExpiresAt, "관리자 세션 절대 만료 시각은 필수입니다.");
		this.idleExpiresAt = requireTime(idleExpiresAt, "관리자 세션 유휴 만료 시각은 필수입니다.");
		this.lastActivityAt = requireTime(now, "관리자 인증 완료 시각은 필수입니다.");
		this.phase = AdminSessionPhase.AUTHENTICATED;
	}

	public boolean belongsTo(String adminId) {
		return this.adminId != null && this.adminId.equals(adminId);
	}

	public boolean isActiveAt(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		return status == AdminSessionStatus.ACTIVE
				&& now.isBefore(absoluteExpiresAt)
				&& now.isBefore(idleExpiresAt);
	}

	public boolean isAuthenticated() {
		return phase == AdminSessionPhase.AUTHENTICATED;
	}

	public boolean hasAuthenticationVersion(long version) {
		return authenticationVersion == version;
	}

	public void touch(LocalDateTime now, LocalDateTime newIdleExpiresAt) {
		requireActive();
		if (!isAuthenticated()) {
			throw new AdminDomainException("인증 완료 세션만 활동 시각을 갱신할 수 있습니다.");
		}
		LocalDateTime requestedIdleExpiresAt = requireTime(newIdleExpiresAt, "새 관리자 세션 유휴 만료 시각은 필수입니다.");
		this.idleExpiresAt = requestedIdleExpiresAt.isAfter(absoluteExpiresAt)
				? absoluteExpiresAt
				: requestedIdleExpiresAt;
		this.lastActivityAt = requireTime(now, "관리자 세션 활동 시각은 필수입니다.");
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

	private void requireActive() {
		if (status != AdminSessionStatus.ACTIVE) {
			throw new AdminDomainException("활성 관리자 세션이 아닙니다.");
		}
	}

	private void requirePhase(AdminSessionPhase expectedPhase) {
		if (phase != requirePhaseValue(expectedPhase)) {
			throw new AdminDomainException("현재 관리자 인증 단계에서 수행할 수 없는 작업입니다.");
		}
	}

	private static AdminSessionPhase requirePhaseValue(AdminSessionPhase phase) {
		if (phase == null) {
			throw new AdminDomainException("관리자 인증 단계는 필수입니다.");
		}
		return phase;
	}

	private static long requireVersion(long version) {
		if (version < 0) {
			throw new AdminDomainException("관리자 인증 버전은 음수일 수 없습니다.");
		}
		return version;
	}

	@Deprecated(forRemoval = true)
	public static AdminSession start(String adminId, String credentialHash,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		AdminSession session = new AdminSession(
				AdminId.newId(), credentialHash, credentialHash, absoluteExpiresAt, idleExpiresAt, now);
		session.bindAdmin(adminId, 0L, AdminSessionPhase.AUTHENTICATED, absoluteExpiresAt, now);
		session.phase = AdminSessionPhase.AUTHENTICATED;
		session.idleExpiresAt = idleExpiresAt;
		return session;
	}

	@Deprecated(forRemoval = true)
	public static AdminSession start(String sessionId, String adminId, String credentialHash,
			LocalDateTime absoluteExpiresAt, LocalDateTime idleExpiresAt, LocalDateTime now) {
		AdminSession session = new AdminSession(
				sessionId, credentialHash, credentialHash, absoluteExpiresAt, idleExpiresAt, now);
		session.bindAdmin(adminId, 0L, AdminSessionPhase.AUTHENTICATED, absoluteExpiresAt, now);
		session.phase = AdminSessionPhase.AUTHENTICATED;
		session.idleExpiresAt = idleExpiresAt;
		return session;
	}

	@Deprecated(forRemoval = true)
	public String getCurrentRefreshTokenHash() {
		return sessionTokenHash;
	}

	@Deprecated(forRemoval = true)
	public void rotate(String credentialHash, LocalDateTime newIdleExpiresAt, LocalDateTime now) {
		this.sessionTokenHash = requireText(credentialHash, "관리자 자격 증명 해시는 필수입니다.");
		this.csrfTokenHash = credentialHash;
		this.idleExpiresAt = requireTime(newIdleExpiresAt, "관리자 세션 유휴 만료 시각은 필수입니다.");
		this.lastActivityAt = requireTime(now, "관리자 세션 변경 시각은 필수입니다.");
	}

	@Deprecated(forRemoval = true)
	public void markReuseDetected(LocalDateTime now) {
		revoke(now);
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
