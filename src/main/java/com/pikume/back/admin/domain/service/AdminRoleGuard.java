package com.pikume.back.admin.domain.service;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.exception.AdminDomainException;

public class AdminRoleGuard {

	public void validateCanDeactivate(AdminAccount actor, AdminAccount target, long activeSuperAdminCount) {
		requireSuperAdmin(actor);
		requireDifferentAccount(actor, target, "관리자가 자기 계정을 비활성화할 수 없습니다.");
		requireNotLastSuperAdmin(target, activeSuperAdminCount, "마지막 SUPER_ADMIN은 비활성화할 수 없습니다.");
	}

	public void validateCanChangeRole(AdminAccount actor, AdminAccount target, AdminRole newRole,
			long activeSuperAdminCount) {
		requireSuperAdmin(actor);
		requireDifferentAccount(actor, target, "관리자가 자기 등급을 변경할 수 없습니다.");
		if (newRole == null) {
			throw new AdminDomainException("변경할 관리자 등급은 필수입니다.");
		}
		if (target.isSuperAdmin() && !newRole.isSuperAdmin()) {
			requireNotLastSuperAdmin(target, activeSuperAdminCount, "마지막 SUPER_ADMIN의 등급을 낮출 수 없습니다.");
		}
	}

	private void requireSuperAdmin(AdminAccount actor) {
		if (actor == null || !actor.isSuperAdmin()) {
			throw new AdminDomainException("SUPER_ADMIN만 수행할 수 있는 작업입니다.");
		}
	}

	private void requireDifferentAccount(AdminAccount actor, AdminAccount target, String message) {
		if (target == null) {
			throw new AdminDomainException("대상 관리자 계정은 필수입니다.");
		}
		if (actor.getId().equals(target.getId())) {
			throw new AdminDomainException(message);
		}
	}

	private void requireNotLastSuperAdmin(AdminAccount target, long activeSuperAdminCount, String message) {
		if (target.isSuperAdmin() && activeSuperAdminCount <= 1) {
			throw new AdminDomainException(message);
		}
	}
}
