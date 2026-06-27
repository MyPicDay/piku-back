package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAccount;

public interface SaveAdminAccountPort {

	AdminAccount save(AdminAccount adminAccount);
}
