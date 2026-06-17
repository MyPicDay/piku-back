package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminOtpRegistrationResult;
import com.pikume.back.admin.application.service.AdminTemporaryLoginResult;
import com.pikume.back.admin.application.service.AdminTokenIssueResult;

public interface AdminOnboardingUseCase {

	AdminTemporaryLoginResult temporaryLogin(String email, String temporaryPassword);

	void setLoginId(String adminId, String loginId);

	void setPassword(String adminId, String password);

	AdminOtpRegistrationResult startOtpRegistration(String adminId);

	AdminTokenIssueResult verifyOtp(String adminId, String otpCode);
}
