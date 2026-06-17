package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminRefreshToken;

public interface SaveAdminRefreshTokenPort {

	AdminRefreshToken save(AdminRefreshToken adminRefreshToken);
}
