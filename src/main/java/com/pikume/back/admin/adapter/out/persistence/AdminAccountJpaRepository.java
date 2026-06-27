package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminAccountJpaRepository extends JpaRepository<AdminAccount, String> {

	Optional<AdminAccount> findByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select admin from AdminAccount admin where admin.id = :adminId")
	Optional<AdminAccount> findByIdForUpdate(@Param("adminId") String adminId);

	Optional<AdminAccount> findByLoginId(String loginId);

	boolean existsByEmail(String email);

	boolean existsByLoginId(String loginId);

	long countByRoleAndStatus(AdminRole role, AdminAccountStatus status);
}
