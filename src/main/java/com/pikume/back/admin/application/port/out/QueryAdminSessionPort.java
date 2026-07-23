package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminSession;

import java.util.List;
import java.util.Optional;

public interface QueryAdminSessionPort {

	Optional<AdminSession> findSession(String sessionId);

	Optional<AdminSession> findSessionByTokenHash(String sessionTokenHash);

	Optional<AdminSession> lockSessionByTokenHash(String sessionTokenHash);

	List<AdminSession> findActiveSessions(String adminId);
}
