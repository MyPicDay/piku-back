package store.piku.back.user.auth.application.port.in;

import store.piku.back.user.auth.dto.request.PwdResetRequest;

public interface ResetPasswordUseCase {

	void verifyCodeAndResetPwd(PwdResetRequest dto);
}
