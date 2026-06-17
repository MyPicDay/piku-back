package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminRefreshToken;
import com.pikume.back.admin.domain.AdminRefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRefreshTokenJpaRepository extends JpaRepository<AdminRefreshToken, String> {

	Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

	List<AdminRefreshToken> findBySessionIdAndStatus(String sessionId, AdminRefreshTokenStatus status);
}
