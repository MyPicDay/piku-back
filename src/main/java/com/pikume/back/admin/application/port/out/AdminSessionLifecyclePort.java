package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.application.service.AdminSessionCredentials;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminSessionPhase;

import java.time.LocalDateTime;

public interface AdminSessionLifecyclePort {

	void bindPreAuthentication(String rawSessionToken, String adminId, long authenticationVersion,
			AdminSessionPhase nextPhase, LocalDateTime now);

	AdminAccount requirePhaseForUpdate(
			String rawSessionToken, AdminSessionPhase expectedPhase, LocalDateTime now);

	String advancePhase(String rawSessionToken, AdminSessionPhase expectedPhase,
			AdminSessionPhase nextPhase, long currentAuthenticationVersion, LocalDateTime now);

	AdminSessionCredentials completeAuthentication(String rawSessionToken, AdminAccount admin,
			AdminSessionPhase expectedPhase, LocalDateTime now);

	void revokeActiveSessions(String adminId, LocalDateTime now);

	void revokeCurrent(String adminId, String sessionId, LocalDateTime now);
}
