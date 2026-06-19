package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminSession;

import java.util.List;
import java.util.Optional;

public interface LoadAdminSessionPort {

	Optional<AdminSession> findById(String sessionId);

	Optional<AdminSession> findBySessionTokenHash(String sessionTokenHash);

	Optional<AdminSession> findBySessionTokenHashForUpdate(String sessionTokenHash);

	List<AdminSession> findActiveByAdminId(String adminId);
}
