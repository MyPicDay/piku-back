package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;

import java.util.Optional;

public interface QueryAdminAccountPort {

	Optional<AdminAccount> findAccount(String adminId);

	Optional<AdminAccount> lockAccount(String adminId);

	Optional<AdminAccount> findAccountByEmail(String email);

	Optional<AdminAccount> findAccountByLoginId(String loginId);

	boolean emailAlreadyRegistered(String email);

	boolean loginIdAlreadyRegistered(String loginId);

	long countAccountsByRoleAndStatus(AdminRole role, AdminAccountStatus status);
}
