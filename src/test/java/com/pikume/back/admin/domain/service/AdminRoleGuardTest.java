package com.pikume.back.admin.domain.service;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import com.pikume.back.admin.domain.exception.AdminDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdminRoleGuard")
class AdminRoleGuardTest {

	private final AdminRoleGuard guard = new AdminRoleGuard();
	private final LocalDateTime now = LocalDateTime.of(2026, 6, 17, 13, 0);

	@Test
	@DisplayName("SUPER_ADMIN만 관리자 계정 운영을 수행할 수 있다")
	void onlySuperAdminCanOperateAdminAccounts() {
		AdminAccount actor = admin(AdminRole.OPERATOR, "actor@pikume.com");
		AdminAccount target = admin(AdminRole.VIEWER, "target@pikume.com");

		assertThatThrownBy(() -> guard.validateCanDeactivate(actor, target, 1))
				.isInstanceOf(AdminDomainException.class);
	}

	@Test
	@DisplayName("관리자는 자기 계정을 비활성화할 수 없다")
	void cannotDeactivateSelf() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "actor@pikume.com");

		assertThatThrownBy(() -> guard.validateCanDeactivate(actor, actor, 2))
				.isInstanceOf(AdminDomainException.class);
	}

	@Test
	@DisplayName("마지막 SUPER_ADMIN은 비활성화할 수 없다")
	void cannotDeactivateLastSuperAdmin() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "actor@pikume.com");
		AdminAccount target = admin(AdminRole.SUPER_ADMIN, "target@pikume.com");

		assertThatThrownBy(() -> guard.validateCanDeactivate(actor, target, 1))
				.isInstanceOf(AdminDomainException.class);
	}

	@Test
	@DisplayName("마지막 SUPER_ADMIN의 등급을 낮출 수 없다")
	void cannotDowngradeLastSuperAdmin() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "actor@pikume.com");
		AdminAccount target = admin(AdminRole.SUPER_ADMIN, "target@pikume.com");

		assertThatThrownBy(() -> guard.validateCanChangeRole(actor, target, AdminRole.OPERATOR, 1))
				.isInstanceOf(AdminDomainException.class);
	}

	@Test
	@DisplayName("마지막 SUPER_ADMIN 보호 조건에 걸리지 않으면 운영 작업을 허용한다")
	void allowsOperationWhenPolicyIsSatisfied() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "actor@pikume.com");
		AdminAccount target = admin(AdminRole.OPERATOR, "target@pikume.com");

		assertThatNoException()
				.isThrownBy(() -> guard.validateCanDeactivate(actor, target, 1));
		assertThatNoException()
				.isThrownBy(() -> guard.validateCanChangeRole(actor, target, AdminRole.VIEWER, 1));
	}

	private AdminAccount admin(AdminRole role, String email) {
		return AdminAccount.invite(
				email,
				"관리자1",
				role,
				"temp-hash",
				now,
				now.plusDays(1));
	}
}
