package com.pikume.back.user.auth.application.port.in;

import java.util.List;

public interface QueryAllowedEmailUseCase {

	boolean isEmailAllowed(String email);

	List<String> queryAllowedEmailDomains();
}
