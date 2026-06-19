package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAccount;

public interface SaveAdminCredentialsPort {

	boolean saveIfLoginIdAvailable(AdminAccount adminAccount);
}
