package com.pikume.back.user.auth.application.port.in;

import com.pikume.back.user.auth.dto.request.EmailValidRequest;

public interface VerifyEmailUseCase {

	void sendSignUpVerificationEmail(String email);

	void sendPasswordResetVerificationEmail(String email);

	void verifyCode(EmailValidRequest dto);
}
