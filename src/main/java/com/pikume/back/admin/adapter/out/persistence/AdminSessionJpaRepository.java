package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionPhase;
import com.pikume.back.admin.domain.AdminSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminSessionJpaRepository extends JpaRepository<AdminSession, String> {

	List<AdminSession> findByAdminIdAndStatus(String adminId, AdminSessionStatus status);

	Optional<AdminSession> findBySessionTokenHash(String sessionTokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from AdminSession session where session.sessionTokenHash = :sessionTokenHash")
	Optional<AdminSession> findBySessionTokenHashForUpdate(
			@Param("sessionTokenHash") String sessionTokenHash);

	long countByStatusAndPhaseAndAbsoluteExpiresAtAfterAndIdleExpiresAtAfter(
			AdminSessionStatus status,
			AdminSessionPhase phase,
			LocalDateTime absoluteExpiresAt,
			LocalDateTime idleExpiresAt);
}
