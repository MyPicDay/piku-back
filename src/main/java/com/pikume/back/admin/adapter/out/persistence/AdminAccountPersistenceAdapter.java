package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.SaveAdminAccountPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminAccountPersistenceAdapter implements LoadAdminAccountPort, SaveAdminAccountPort {

	private final AdminAccountJpaRepository adminAccountJpaRepository;

	@Override
	public Optional<AdminAccount> findById(String adminId) {
		return adminAccountJpaRepository.findById(adminId);
	}

	@Override
	public Optional<AdminAccount> findByEmail(String email) {
		return adminAccountJpaRepository.findByEmail(email);
	}

	@Override
	public Optional<AdminAccount> findByLoginId(String loginId) {
		return adminAccountJpaRepository.findByLoginId(loginId);
	}

	@Override
	public boolean existsByEmail(String email) {
		return adminAccountJpaRepository.existsByEmail(email);
	}

	@Override
	public boolean existsByLoginId(String loginId) {
		return adminAccountJpaRepository.existsByLoginId(loginId);
	}

	@Override
	public long countByRoleAndStatus(AdminRole role, AdminAccountStatus status) {
		return adminAccountJpaRepository.countByRoleAndStatus(role, status);
	}

	@Override
	public AdminAccount save(AdminAccount adminAccount) {
		return adminAccountJpaRepository.save(adminAccount);
	}
}
