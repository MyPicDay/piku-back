package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAuditLog;

public interface SaveAdminAuditLogPort {

	AdminAuditLog save(AdminAuditLog auditLog);
}
