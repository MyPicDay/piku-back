package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import com.pikume.back.admin.application.dto.AuthenticatedAdminSessionResult;

import java.time.LocalDateTime;

public interface AdminSessionSecurityUseCase {

	AdminSessionCredentialResult initialize(LocalDateTime now);

	AuthenticatedAdminSessionResult authenticate(String rawSessionToken, LocalDateTime now);

	void validateCsrf(String rawSessionToken, String rawCsrfToken, LocalDateTime now);
}
