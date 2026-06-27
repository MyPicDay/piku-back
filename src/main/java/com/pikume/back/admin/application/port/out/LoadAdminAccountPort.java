package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;

import java.util.Optional;

public interface LoadAdminAccountPort {

	Optional<AdminAccount> findById(String adminId);

	Optional<AdminAccount> findByIdForUpdate(String adminId);

	Optional<AdminAccount> findByEmail(String email);

	Optional<AdminAccount> findByLoginId(String loginId);

	boolean existsByEmail(String email);

	boolean existsByLoginId(String loginId);

	long countByRoleAndStatus(AdminRole role, AdminAccountStatus status);
}
