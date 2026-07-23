package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.AdminAccountOperationUseCase;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.in.ManageAdminSessionLifecycleUseCase;
import com.pikume.back.admin.application.port.out.GenerateTemporaryPasswordPort;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.QueryAdminAuditTrailPort;
import com.pikume.back.admin.application.port.out.AppendAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.RecordAdminAccountPort;
import com.pikume.back.admin.application.port.out.SearchAdminAccountsPort;
import com.pikume.back.admin.application.port.out.SendAdminGuideEmailPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminAuditAction;
import com.pikume.back.admin.domain.AdminAuditLog;
import com.pikume.back.admin.domain.AdminEmail;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.exception.AdminDomainException;
import com.pikume.back.admin.domain.service.AdminRoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccountOperationService implements AdminAccountOperationUseCase {

	private static final int AUDIT_LOG_LIMIT_MAX = 200;
	private static final AdminRoleGuard ADMIN_ROLE_GUARD = new AdminRoleGuard();

	private final QueryAdminAccountPort queryAdminAccountPort;
	private final RecordAdminAccountPort recordAdminAccountPort;
	private final SearchAdminAccountsPort searchAdminAccountsPort;
	private final AppendAdminAuditLogPort appendAdminAuditLogPort;
	private final QueryAdminAuditTrailPort queryAdminAuditTrailPort;
	private final GenerateTemporaryPasswordPort generateTemporaryPasswordPort;
	private final SendAdminGuideEmailPort sendAdminGuideEmailPort;
	private final AdminPasswordPort adminPasswordPort;
	private final ManageAdminSessionLifecycleUseCase adminSessionLifecyclePort;

	@Override
	@Transactional(readOnly = true)
	public List<AdminAccountSummaryResult> list(String actorAdminId) {
		requireSuperAdmin(actorAdminId);
		return searchAdminAccountsPort.queryAccounts()
				.stream()
				.map(AdminAccountSummaryResult::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AdminAccountDetailResult detailById(String actorAdminId, String targetAdminId) {
		requireSuperAdmin(actorAdminId);
		return queryAdminAccountPort.findAccount(targetAdminId)
				.map(AdminAccountDetailResult::from)
				.orElseThrow(this::notFound);
	}

	@Override
	@Transactional
	public void changeRole(String actorAdminId, String targetAdminId, AdminRole role) {
		if (role == null) {
			throw new AdminException(AdminErrorCode.INVALID_REQUEST, "변경할 관리자 등급은 필수입니다.");
		}
		AdminAccount actor = requireSuperAdmin(actorAdminId);
		AdminAccount target = requireTarget(targetAdminId);
		validateCanChangeRole(actor, target, role);
		AdminRole before = target.getRole();
		target.changeRole(role);
		adminSessionLifecyclePort.revokeActiveSessions(target.getId(), LocalDateTime.now());
		audit(actorAdminId, target.getId(), AdminAuditAction.ROLE_CHANGED, null,
				"role: %s -> %s".formatted(before, role));
	}

	@Override
	@Transactional
	public void deactivate(String actorAdminId, String targetAdminId, String reason) {
		AdminAccount actor = requireSuperAdmin(actorAdminId);
		if (!StringUtils.hasText(reason)) {
			throw new AdminException(AdminErrorCode.INVALID_REQUEST, "관리자 계정 비활성화 사유는 필수입니다.");
		}
		AdminAccount target = requireTarget(targetAdminId);
		validateCanDeactivate(actor, target);
		target.deactivate(reason);
		adminSessionLifecyclePort.revokeActiveSessions(target.getId(), LocalDateTime.now());
		audit(actorAdminId, target.getId(), AdminAuditAction.DEACTIVATED, reason, null);
	}

	@Override
	@Transactional
	public void reactivate(String actorAdminId, String targetAdminId) {
		requireSuperAdmin(actorAdminId);
		AdminAccount target = requireTarget(targetAdminId);
		target.reactivate();
		audit(actorAdminId, target.getId(), AdminAuditAction.REACTIVATED, null, null);
	}

	@Override
	@Transactional
	public void unlock(String actorAdminId, String targetAdminId) {
		requireSuperAdmin(actorAdminId);
		AdminAccount target = requireTarget(targetAdminId);
		target.unlock();
		audit(actorAdminId, target.getId(), AdminAuditAction.UNLOCKED, null, null);
	}

	@Override
	@Transactional
	public AdminTemporaryPasswordResult reissueTemporaryPassword(String actorAdminId, String targetAdminId) {
		requireSuperAdmin(actorAdminId);
		AdminAccount target = requireTarget(targetAdminId);
		if (target.hasLoginId()) {
			throw new AdminException(AdminErrorCode.INVALID_REQUEST, "정식 로그인 아이디 설정 이후에는 임시 패스워드를 재발급할 수 없습니다.");
		}
		AdminTemporaryPasswordResult result = reissueTemporaryPassword(target);
		adminSessionLifecyclePort.revokeActiveSessions(target.getId(), LocalDateTime.now());
		audit(actorAdminId, target.getId(), AdminAuditAction.TEMPORARY_PASSWORD_REISSUED, null, null);
		return result;
	}

	@Override
	@Transactional
	public void resetOtp(String actorAdminId, String targetAdminId) {
		requireSuperAdmin(actorAdminId);
		AdminAccount target = requireTarget(targetAdminId);
		target.resetOtp();
		adminSessionLifecyclePort.revokeActiveSessions(target.getId(), LocalDateTime.now());
		audit(actorAdminId, target.getId(), AdminAuditAction.OTP_RESET, null, null);
	}

	@Override
	@Transactional
	public void changeEmail(String actorAdminId, String targetAdminId, String newEmail) {
		requireSuperAdmin(actorAdminId);
		AdminAccount target = requireTarget(targetAdminId);
		String normalizedEmail = AdminEmail.normalize(newEmail);
		if (!target.getEmail().equals(normalizedEmail) && queryAdminAccountPort.emailAlreadyRegistered(normalizedEmail)) {
			throw new AdminException(AdminErrorCode.DUPLICATE_EMAIL, "이미 등록된 관리자 이메일입니다.");
		}
		target.changeEmail(normalizedEmail);
		audit(actorAdminId, target.getId(), AdminAuditAction.EMAIL_CHANGED, null, "email changed");
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminAuditLogResult> auditLogs(String actorAdminId, int limit) {
		requireSuperAdmin(actorAdminId);
		int boundedLimit = Math.max(1, Math.min(limit, AUDIT_LOG_LIMIT_MAX));
		return queryAdminAuditTrailPort.queryLatestEntries(boundedLimit)
				.stream()
				.map(AdminAuditLogResult::from)
				.toList();
	}

	private AdminTemporaryPasswordResult reissueTemporaryPassword(AdminAccount target) {
		String temporaryPassword = generateTemporaryPasswordPort.generate();
		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusHours(24);
		target.reissueTemporaryPassword(adminPasswordPort.encode(temporaryPassword), issuedAt, expiresAt);
		recordAdminAccountPort.recordAccount(target);
		boolean guideEmailSent = true;
		try {
			sendAdminGuideEmailPort.sendAccountCreatedGuide(target.getEmail(), target.getEmail(), expiresAt);
		} catch (RuntimeException e) {
			guideEmailSent = false;
		}
		return new AdminTemporaryPasswordResult(temporaryPassword, expiresAt, guideEmailSent);
	}

	private AdminAccount requireSuperAdmin(String actorAdminId) {
		AdminAccount actor = queryAdminAccountPort.findAccount(actorAdminId)
				.orElseThrow(() -> new AdminException(AdminErrorCode.FORBIDDEN, "SUPER_ADMIN 권한이 필요합니다."));
		if (!actor.isSuperAdmin()) {
			throw new AdminException(AdminErrorCode.FORBIDDEN, "SUPER_ADMIN 권한이 필요합니다.");
		}
		return actor;
	}

	private AdminAccount requireTarget(String targetAdminId) {
		return queryAdminAccountPort.findAccount(targetAdminId)
				.orElseThrow(this::notFound);
	}

	private AdminException notFound() {
		return new AdminException(AdminErrorCode.NOT_FOUND, "관리자 계정을 찾을 수 없습니다.");
	}

	private void validateCanChangeRole(AdminAccount actor, AdminAccount target, AdminRole role) {
		try {
			ADMIN_ROLE_GUARD.validateCanChangeRole(actor, target, role, activeSuperAdminCount());
		} catch (AdminDomainException e) {
			throw new AdminException(AdminErrorCode.FORBIDDEN, e.getMessage());
		}
	}

	private void validateCanDeactivate(AdminAccount actor, AdminAccount target) {
		try {
			ADMIN_ROLE_GUARD.validateCanDeactivate(actor, target, activeSuperAdminCount());
		} catch (AdminDomainException e) {
			throw new AdminException(AdminErrorCode.FORBIDDEN, e.getMessage());
		}
	}

	private long activeSuperAdminCount() {
		return queryAdminAccountPort.countAccountsByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminAccountStatus.ACTIVE);
	}

	private void audit(String actorAdminId, String targetAdminId, AdminAuditAction action, String reason, String detail) {
		appendAdminAuditLogPort.appendAuditLog(AdminAuditLog.record(
				actorAdminId,
				targetAdminId,
				action,
				AdminIdentifierMasker.removeEmailsFromText(reason),
				AdminIdentifierMasker.removeEmailsFromText(detail),
				LocalDateTime.now()));
	}
}
