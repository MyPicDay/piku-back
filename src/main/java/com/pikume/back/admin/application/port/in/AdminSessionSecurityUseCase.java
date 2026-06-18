package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminSessionCredentials;
import com.pikume.back.admin.application.service.AuthenticatedAdminSession;

import java.time.LocalDateTime;

public interface AdminSessionSecurityUseCase {

	AdminSessionCredentials initialize(LocalDateTime now);

	AuthenticatedAdminSession authenticate(String rawSessionToken, LocalDateTime now);

	void validateCsrf(String rawSessionToken, String rawCsrfToken, LocalDateTime now);
}
