package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminAuditAction;
import com.pikume.back.admin.domain.AdminAuditLog;

import java.time.LocalDateTime;

public record AdminAuditLogResult(
		Long id,
		String actorAdminId,
		String targetAdminId,
		AdminAuditAction action,
		String reason,
		String detail,
		LocalDateTime occurredAt
) {

	public static AdminAuditLogResult from(AdminAuditLog auditLog) {
		return new AdminAuditLogResult(
				auditLog.getId(),
				auditLog.getActorAdminId(),
				auditLog.getTargetAdminId(),
				auditLog.getAction(),
				auditLog.getReason(),
				auditLog.getDetail(),
				auditLog.getOccurredAt());
	}
}
