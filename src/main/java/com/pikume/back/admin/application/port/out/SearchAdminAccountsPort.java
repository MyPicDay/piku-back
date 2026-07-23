package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminAccount;

import java.util.List;

public interface SearchAdminAccountsPort {

	List<AdminAccount> queryAccounts();
}
