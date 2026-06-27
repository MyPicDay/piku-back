package com.pikume.back.admin.adapter.out.persistence;

import com.pikume.back.admin.application.port.out.LoadAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.SaveAdminAuditLogPort;
import com.pikume.back.admin.domain.AdminAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminAuditLogPersistenceAdapter implements SaveAdminAuditLogPort, LoadAdminAuditLogPort {

	private final AdminAuditLogJpaRepository adminAuditLogJpaRepository;

	@Override
	public AdminAuditLog save(AdminAuditLog auditLog) {
		return adminAuditLogJpaRepository.save(auditLog);
	}

	@Override
	public List<AdminAuditLog> findLatest(int limit) {
		return adminAuditLogJpaRepository.findByOrderByOccurredAtDesc(PageRequest.of(0, limit));
	}
}
