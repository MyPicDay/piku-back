package com.pikume.back.admin.application.port.out;

import java.time.LocalDateTime;

public interface CountAdminSessionsPort {

	long countActiveAuthenticatedAt(LocalDateTime now);
}
