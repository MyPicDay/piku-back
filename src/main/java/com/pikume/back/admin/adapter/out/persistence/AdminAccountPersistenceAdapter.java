package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.CommitAdminCredentialsPort;
import com.pikume.back.admin.application.port.out.RecordAdminAccountPort;
import com.pikume.back.admin.application.port.out.SearchAdminAccountsPort;
import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminAccountPersistenceAdapter implements
		QueryAdminAccountPort,
		RecordAdminAccountPort,
		CommitAdminCredentialsPort,
		SearchAdminAccountsPort {

	private final AdminAccountJpaRepository adminAccountJpaRepository;

	@Override
	public Optional<AdminAccount> findAccount(String adminId) {
		return adminAccountJpaRepository.findById(adminId);
	}

	@Override
	public Optional<AdminAccount> lockAccount(String adminId) {
		return adminAccountJpaRepository.findByIdForUpdate(adminId);
	}

	@Override
	public Optional<AdminAccount> findAccountByEmail(String email) {
		return adminAccountJpaRepository.findByEmail(email);
	}

	@Override
	public Optional<AdminAccount> findAccountByLoginId(String loginId) {
		return adminAccountJpaRepository.findByLoginId(loginId);
	}

	@Override
	public boolean emailAlreadyRegistered(String email) {
		return adminAccountJpaRepository.existsByEmail(email);
	}

	@Override
	public boolean loginIdAlreadyRegistered(String loginId) {
		return adminAccountJpaRepository.existsByLoginId(loginId);
	}

	@Override
	public long countAccountsByRoleAndStatus(AdminRole role, AdminAccountStatus status) {
		return adminAccountJpaRepository.countByRoleAndStatus(role, status);
	}

	@Override
	public List<AdminAccount> queryAccounts() {
		return adminAccountJpaRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Override
	public AdminAccount recordAccount(AdminAccount adminAccount) {
		return adminAccountJpaRepository.save(adminAccount);
	}

	@Override
	public boolean commitIfLoginIdAvailable(AdminAccount adminAccount) {
		try {
			adminAccountJpaRepository.saveAndFlush(adminAccount);
			return true;
		} catch (DataIntegrityViolationException exception) {
			return false;
		} catch (DataAccessException exception) {
			throw new AdminAuthenticationStoreException(
					"관리자 인증 저장소를 사용할 수 없습니다.", exception);
		}
	}
}
