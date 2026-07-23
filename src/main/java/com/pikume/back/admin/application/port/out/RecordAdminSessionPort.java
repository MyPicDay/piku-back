package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminSession;

public interface RecordAdminSessionPort {

	AdminSession recordSession(AdminSession adminSession);
}
