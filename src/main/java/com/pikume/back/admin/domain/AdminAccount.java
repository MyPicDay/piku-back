package com.pikume.back.admin.domain;

import com.pikume.back.admin.domain.exception.AdminDomainException;
import com.pikume.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@SecondaryTable(name = "admin_roles", pkJoinColumns = @PrimaryKeyJoinColumn(name = "admin_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount extends BaseEntity {

	private static final int PASSWORD_FAILURE_LIMIT = 5;
	private static final int OTP_FAILURE_LIMIT = 5;

	@Id
	@Column(length = 36)
	private String id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "login_id", length = 15, unique = true)
	private String loginId;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(table = "admin_roles", name = "role", nullable = false, length = 30)
	private AdminRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AdminAccountStatus status;

	@Column(name = "temporary_password_hash")
	private String temporaryPasswordHash;

	@Column(name = "temporary_password_issued_at")
	private LocalDateTime temporaryPasswordIssuedAt;

	@Column(name = "temporary_credential_expires_at")
	private LocalDateTime temporaryCredentialExpiresAt;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "password_change_required", nullable = false)
	private boolean passwordChangeRequired;

	@Column(name = "otp_registered", nullable = false)
	private boolean otpRegistered;

	@Column(name = "otp_registration_required", nullable = false)
	private boolean otpRegistrationRequired;

	@Column(name = "login_failure_count", nullable = false)
	private int loginFailureCount;

	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;

	@Column(name = "otp_failure_count", nullable = false)
	private int otpFailureCount;

	@Column(name = "otp_blocked_until")
	private LocalDateTime otpBlockedUntil;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	private AdminAccount(String email, String nickname, AdminRole role, String temporaryPasswordHash,
			LocalDateTime temporaryPasswordIssuedAt, LocalDateTime temporaryCredentialExpiresAt) {
		this.id = AdminId.newId();
		this.email = AdminEmail.normalize(email);
		this.nickname = AdminNickname.normalize(nickname);
		this.role = requireRole(role);
		this.status = AdminAccountStatus.ACTIVE;
		this.temporaryPasswordHash = requireHash(temporaryPasswordHash, "임시 패스워드 해시는 필수입니다.");
		this.temporaryPasswordIssuedAt = requireTime(temporaryPasswordIssuedAt, "임시 패스워드 발급 시각은 필수입니다.");
		this.temporaryCredentialExpiresAt = requireTime(temporaryCredentialExpiresAt, "임시 자격 증명 만료 시각은 필수입니다.");
		this.passwordChangeRequired = true;
		this.otpRegistered = false;
		this.otpRegistrationRequired = true;
	}

	public static AdminAccount invite(String email, String nickname, AdminRole role, String temporaryPasswordHash,
			LocalDateTime issuedAt, LocalDateTime expiresAt) {
		return new AdminAccount(email, nickname, role, temporaryPasswordHash, issuedAt, expiresAt);
	}

	public boolean hasLoginId() {
		return loginId != null;
	}

	public boolean isSuperAdmin() {
		return role.isSuperAdmin();
	}

	public boolean canUseTemporaryCredentialAt(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		return temporaryPasswordHash != null
				&& temporaryCredentialExpiresAt != null
				&& !now.isAfter(temporaryCredentialExpiresAt)
				&& status == AdminAccountStatus.ACTIVE;
	}

	public boolean isLockedAt(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		return status == AdminAccountStatus.LOCKED
				&& lockedUntil != null
				&& now.isBefore(lockedUntil);
	}

	public boolean isOtpBlockedAt(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		return otpBlockedUntil != null && now.isBefore(otpBlockedUntil);
	}

	public void setLoginId(String loginId) {
		if (this.loginId != null) {
			throw new AdminDomainException("정식 로그인 아이디는 변경할 수 없습니다.");
		}
		this.loginId = AdminLoginId.normalize(loginId);
	}

	public void completePasswordSetup(String passwordHash) {
		this.passwordHash = requireHash(passwordHash, "패스워드 해시는 필수입니다.");
		this.passwordChangeRequired = false;
		this.temporaryPasswordHash = null;
		this.temporaryCredentialExpiresAt = null;
	}

	public void completeOtpRegistration() {
		this.otpRegistered = true;
		this.otpRegistrationRequired = false;
		resetOtpFailures();
	}

	public void recordLoginSuccess(LocalDateTime now) {
		requireTime(now, "로그인 성공 시각은 필수입니다.");
		this.status = AdminAccountStatus.ACTIVE;
		this.loginFailureCount = 0;
		this.lockedUntil = null;
		this.lastLoginAt = now;
	}

	public void recordPasswordFailure(LocalDateTime now) {
		requireTime(now, "로그인 실패 시각은 필수입니다.");
		this.loginFailureCount++;
		if (loginFailureCount >= PASSWORD_FAILURE_LIMIT) {
			this.status = AdminAccountStatus.LOCKED;
			this.lockedUntil = now.plusMinutes(30);
		}
	}

	public void releaseExpiredLock(LocalDateTime now) {
		requireTime(now, "현재 시각은 필수입니다.");
		if (status == AdminAccountStatus.LOCKED && lockedUntil != null && !now.isBefore(lockedUntil)) {
			unlock();
		}
	}

	public void unlock() {
		this.status = AdminAccountStatus.ACTIVE;
		this.loginFailureCount = 0;
		this.lockedUntil = null;
		resetOtpFailures();
	}

	public void recordOtpFailure(LocalDateTime now) {
		requireTime(now, "OTP 실패 시각은 필수입니다.");
		this.otpFailureCount++;
		if (otpFailureCount >= OTP_FAILURE_LIMIT) {
			this.otpBlockedUntil = now.plusMinutes(10);
		}
	}

	public void resetOtpFailures() {
		this.otpFailureCount = 0;
		this.otpBlockedUntil = null;
	}

	public void deactivate(String reason) {
		if (reason == null || reason.isBlank()) {
			throw new AdminDomainException("관리자 계정 비활성화 사유는 필수입니다.");
		}
		this.status = AdminAccountStatus.INACTIVE;
	}

	public void reactivate(String temporaryPasswordHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
		this.status = AdminAccountStatus.ACTIVE;
		this.loginFailureCount = 0;
		this.lockedUntil = null;
		reissueTemporaryPassword(temporaryPasswordHash, issuedAt, expiresAt);
		this.passwordChangeRequired = true;
	}

	public void reissueTemporaryPassword(String temporaryPasswordHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
		this.temporaryPasswordHash = requireHash(temporaryPasswordHash, "임시 패스워드 해시는 필수입니다.");
		this.temporaryPasswordIssuedAt = requireTime(issuedAt, "임시 패스워드 발급 시각은 필수입니다.");
		this.temporaryCredentialExpiresAt = requireTime(expiresAt, "임시 자격 증명 만료 시각은 필수입니다.");
	}

	public void resetOtp() {
		this.otpRegistered = false;
		this.otpRegistrationRequired = true;
		resetOtpFailures();
	}

	public void changeEmail(String newEmail) {
		this.email = AdminEmail.normalize(newEmail);
	}

	public void changeRole(AdminRole newRole) {
		this.role = requireRole(newRole);
	}

	private static AdminRole requireRole(AdminRole role) {
		if (role == null) {
			throw new AdminDomainException("관리자 등급은 필수입니다.");
		}
		return role;
	}

	private static String requireHash(String hash, String message) {
		if (hash == null || hash.isBlank()) {
			throw new AdminDomainException(message);
		}
		return hash;
	}

	private static LocalDateTime requireTime(LocalDateTime time, String message) {
		if (time == null) {
			throw new AdminDomainException(message);
		}
		return time;
	}
}
