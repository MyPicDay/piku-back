package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.CreateAdminAccountCommand;
import com.pikume.back.admin.application.service.CreateAdminAccountResult;

public interface CreateAdminAccountUseCase {

	CreateAdminAccountResult create(CreateAdminAccountCommand command);
}
