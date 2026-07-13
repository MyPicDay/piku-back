package com.pikume.back.user.auth.application.port.in;

import com.pikume.back.user.auth.application.dto.LoginCommand;
import com.pikume.back.user.auth.application.dto.LoginResult;

public interface LoginUseCase {
	LoginResult login(LoginCommand command);
}
