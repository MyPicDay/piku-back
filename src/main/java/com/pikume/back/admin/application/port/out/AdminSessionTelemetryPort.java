package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminSessionPhase;

import java.time.LocalDateTime;

public interface AdminSessionTelemetryPort {

	void cacheHit();

	void databaseFallback();

	void sessionStoreUnavailable();

	void sessionIssued();

	void sessionRevoked();

	void csrfRejected(String reason);

	void originRejected();

	void cacheOperationFailed(String operation);

	void loginSucceeded(String flow, String adminId);

	void loginRejected(String flow, String reason);

	void otpSucceeded(String flow, String adminId);

	void otpRejected(String flow, String reason);

	void phaseChanged(String sessionId, String adminId, AdminSessionPhase fromPhase,
			AdminSessionPhase toPhase, LocalDateTime transitionedAt);
}
