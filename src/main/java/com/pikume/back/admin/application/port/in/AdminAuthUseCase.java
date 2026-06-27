package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminLoginChallengeResult;
import com.pikume.back.admin.application.service.AdminAuthenticationResult;

public interface AdminAuthUseCase {

	AdminLoginChallengeResult login(String sessionToken, String loginId, String password);

	AdminAuthenticationResult verifyOtp(String sessionToken, String otpCode);

	void logout(String adminId, String sessionId);

	void changePassword(String adminId, String currentPassword, String newPassword);
}
