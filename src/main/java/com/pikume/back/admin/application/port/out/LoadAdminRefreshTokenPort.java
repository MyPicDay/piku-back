package com.pikume.back.admin.application.port.out;

import com.pikume.back.admin.domain.AdminRefreshToken;

import java.util.List;
import java.util.Optional;

public interface LoadAdminRefreshTokenPort {

	Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

	List<AdminRefreshToken> findActiveBySessionId(String sessionId);
}
