package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminOtpRegistrationResult;
import com.pikume.back.admin.application.service.AdminTemporaryLoginResult;
import com.pikume.back.admin.application.service.AdminAuthenticationResult;

public interface AdminOnboardingUseCase {

	AdminTemporaryLoginResult temporaryLogin(String sessionToken, String email, String temporaryPassword);

	void setLoginId(String sessionToken, String loginId);

	void setPassword(String sessionToken, String password);

	AdminOtpRegistrationResult startOtpRegistration(String sessionToken);

	AdminAuthenticationResult verifyOtp(String sessionToken, String otpCode);
}
