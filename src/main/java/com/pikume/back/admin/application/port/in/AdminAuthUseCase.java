package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminLoginChallengeResult;
import com.pikume.back.admin.application.service.AdminTokenIssueResult;

public interface AdminAuthUseCase {

	AdminLoginChallengeResult login(String loginId, String password);

	AdminTokenIssueResult verifyOtp(String adminId, String otpCode);

	AdminTokenIssueResult reissue(String refreshToken);

	void logout(String adminId, String sessionId);

	void changePassword(String adminId, String currentPassword, String newPassword);
}
