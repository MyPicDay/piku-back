package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogJpaRepository extends JpaRepository<AdminAuditLog, Long> {

	List<AdminAuditLog> findByOrderByOccurredAtDesc(Pageable pageable);
}
