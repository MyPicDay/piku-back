package com.pikume.back.user.auth.application.port.out;

import java.util.Optional;

public interface RefreshSessionPort {

	record RefreshSession(String key, String refreshToken, String userId) {
	}

	Optional<RefreshSession> findByRefreshToken(String refreshToken);
	void save(RefreshSession session);
	void deleteByRefreshToken(String refreshToken);
	void deleteByKey(String key);
}
