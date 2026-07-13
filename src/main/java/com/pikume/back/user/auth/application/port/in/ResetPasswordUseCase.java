package com.pikume.back.user.auth.application.port.in;

import com.pikume.back.user.auth.application.dto.ResetPasswordCommand;

public interface ResetPasswordUseCase {

	void resetPassword(ResetPasswordCommand command);
}
