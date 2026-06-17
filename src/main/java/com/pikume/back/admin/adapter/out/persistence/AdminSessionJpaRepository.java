package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.domain.AdminSession;
import com.pikume.back.admin.domain.AdminSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminSessionJpaRepository extends JpaRepository<AdminSession, String> {

	List<AdminSession> findByAdminIdAndStatus(String adminId, AdminSessionStatus status);
}
