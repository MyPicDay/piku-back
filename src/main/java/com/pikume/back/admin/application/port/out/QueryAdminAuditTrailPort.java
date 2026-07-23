package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAuditLog;

import java.util.List;

public interface QueryAdminAuditTrailPort {

	List<AdminAuditLog> queryLatestEntries(int limit);
}
