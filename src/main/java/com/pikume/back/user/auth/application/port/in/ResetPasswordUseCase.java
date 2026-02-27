package com.pikume.back.user.auth.application.port.in;

import com.pikume.back.user.auth.dto.request.PwdResetRequest;

public interface ResetPasswordUseCase {

	void verifyCodeAndResetPwd(PwdResetRequest dto);
}
