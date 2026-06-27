package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminSession;

public interface SaveAdminSessionPort {

	AdminSession save(AdminSession adminSession);
}
