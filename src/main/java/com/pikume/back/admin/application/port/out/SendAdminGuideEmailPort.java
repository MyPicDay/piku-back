package com.pikume.back.admin.application.port.out;

import java.time.LocalDateTime;

public interface SendAdminGuideEmailPort {

	void sendAccountCreatedGuide(String email, String temporaryLoginId, LocalDateTime temporaryCredentialExpiresAt);
}
