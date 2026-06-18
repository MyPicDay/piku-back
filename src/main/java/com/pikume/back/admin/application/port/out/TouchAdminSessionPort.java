package com.pikume.back.admin.application.port.out;

import java.time.LocalDateTime;

public interface TouchAdminSessionPort {

	boolean touchAuthenticated(
			String sessionTokenHash,
			long authenticationVersion,
			LocalDateTime now,
			LocalDateTime newIdleExpiresAt);
}
